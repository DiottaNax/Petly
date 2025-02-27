package com.unibo.petly.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unibo.petly.R
import com.unibo.petly.data.database.Cleaning
import com.unibo.petly.data.database.Feeding
import com.unibo.petly.data.database.Pet
import com.unibo.petly.data.database.Received
import com.unibo.petly.data.repositories.CleaningRepository
import com.unibo.petly.data.repositories.FeedingRepository
import com.unibo.petly.data.repositories.PetRepository
import com.unibo.petly.data.repositories.ReceivedRepository
import com.unibo.petly.data.repositories.SpeciesRepository
import com.unibo.petly.utils.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

data class PetCard(
    val pet: Pet,
    val activity: String,
    val date: LocalDate?
)

const val FEEDING = "feeding"
const val CLEANING = "cleaning"

class PetViewModel : ViewModel(), KoinComponent {
    private val petRepository: PetRepository by inject()
    private val feedingRepository: FeedingRepository by inject()
    private val cleaningRepository: CleaningRepository by inject()
    private val speciesRepository: SpeciesRepository by inject()
    private val receivedRepository: ReceivedRepository by inject()

    private var _petSelected: Pet? = null

    val petSelected
        get() = _petSelected

    fun selectPet(pet: Pet) {
        _petSelected = pet
    }

    fun insertPet(pet: Pet, context: Context) = viewModelScope.launch {
        val newPet = pet.copy(petId = petRepository.insert(pet).toInt())
        insertFeeding(newPet, LocalDate.now().minusDays(1), context = context)
        insertCleaning(newPet, LocalDate.now().minusDays(1), context = context)

        val petCount = petRepository.countByUser(pet.userId)!!

        when {
            petCount >= 10 -> {
                insertBadge(context, context.getString(R.string.badge_ten_pets_name), pet.userId)
            }

            petCount >= 5 -> {
                insertBadge(context, context.getString(R.string.badge_five_pets_name), pet.userId)
            }

            petCount >= 1 -> {
                insertBadge(context, context.getString(R.string.badge_first_pet_name), pet.userId)
            }

            else -> { /* No action */
            }
        }
    }

    fun addLike(petId : Int) = viewModelScope.launch {
        petRepository.insertLike(petId)
    }

    fun removeLike(petId : Int) = viewModelScope.launch {
        petRepository.removeLike(petId)
    }

    fun removePet(pet : Pet) = viewModelScope.launch {
        petRepository.removePet(pet.petId)
    }

    fun insertFeeding(pet: Pet, date: LocalDate = LocalDate.now(), context: Context) =
        viewModelScope.launch {
            feedingRepository.insert(Feeding(pet.petId, date))

            val feedingTimes = feedingRepository.getFeedingTimes(pet.userId)
            when {
                 feedingTimes >= 100 -> {
                    insertBadge(context, context.getString(R.string.badge_feeding_guru_name), pet.userId)
                }
                feedingTimes >= 30 -> {
                    insertBadge(context, context.getString(R.string.badge_feeding_bro_name), pet.userId)
                }
                feedingTimes >= 2 -> {
                    insertBadge(context, context.getString(R.string.badge_first_feeding_name), pet.userId)
                }
                else -> { /* No action */ }
            }
        }

    fun getAllFeeding(userId: Int) = feedingRepository.getAll(userId).map { pets ->
        pets.map { pet ->
            PetCard(
                pet = pet,
                activity = FEEDING,
                date = getNextFeeding(pet)
            )
        }
    }

    fun getFeedingBeforeDate(userId: Int, date: LocalDate)
            = getAllFeeding(userId).map{ pets ->
        pets.filter { pet -> pet.date!! <= date }
    }

    fun getFeedingBeforeDateFavorite(userId: Int, date: LocalDate)
        = getAllFeedingFavorite(userId).map{ pets ->
            pets.filter { pet -> pet.date!! <= date }
    }

    fun getAllFeedingFavorite(userId: Int) = feedingRepository.getAllFavorites(userId).map { pets ->
        pets.map { pet ->
            PetCard(
                pet = pet,
                activity = FEEDING,
                date = getNextFeeding(pet)
            )
        }
    }

    fun removeFeeding(petId: Int, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        feedingRepository.remove(petId, date)
    }

    suspend fun getNextFeeding(pet: Pet): LocalDate? = withContext(Dispatchers.IO) {
        speciesRepository.getByName(pet.specie)?.feedingFrequency?.let { feedingFrequency ->
            getLastDate(FEEDING,pet).plusDays(feedingFrequency.toLong())
        }
    }

    fun insertCleaning(pet: Pet, date: LocalDate = LocalDate.now(), context: Context) = viewModelScope.launch {
        cleaningRepository.insert(Cleaning(pet.petId, date))
        val cleaningTimes = cleaningRepository.getCleaningTimes(pet.userId)
        when {
            cleaningTimes >= 40 -> {
                insertBadge(context, context.getString(R.string.badge_cleaning_guru_name), pet.userId)
            }
            cleaningTimes >= 15 -> {
                insertBadge(context, context.getString(R.string.badge_cleaning_bro_name), pet.userId)
            }
            cleaningTimes >= 2 -> {
                insertBadge(context, context.getString(R.string.badge_first_cleaning_name), pet.userId)
            }
            else -> { /* No action */ }
        }
    }

    fun getAllCleaning(userId: Int) = cleaningRepository.getAll(userId).map { pets ->
        pets.map { pet ->
            PetCard(
                pet = pet,
                activity = CLEANING,
                date = getNextCleaning(pet)
            )
        }
    }

    fun getCleaningBeforeDate(userId: Int, date: LocalDate)
        = getAllCleaning(userId).map{ pets ->
            pets.filter { pet -> pet.date!! <= date }
    }

    fun getCleaningBeforeDateFavorite(userId: Int, date: LocalDate)
        = getAllCleaningFavorite(userId).map{ pets ->
            pets.filter { pet -> pet.date!! <= date }
    }

    fun getAllCleaningFavorite(userId: Int) =
        cleaningRepository.getAllFavorites(userId).map { pets ->
        pets.map { pet ->
            PetCard(
                pet = pet,
                activity = CLEANING,
                date = getNextCleaning(pet)
            )
        }
    }

    fun removeCleaning(petId: Int, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        cleaningRepository.remove(petId, date)
    }

    suspend fun getNextCleaning(pet: Pet): LocalDate? = withContext(Dispatchers.IO) {
        speciesRepository.getByName(pet.specie)?.cleaningFrequency?.let { cleaningFrequency ->
            getLastDate(CLEANING, pet).plusDays(cleaningFrequency.toLong())
        }
    }

    //species

    fun getAllSpeciesNames() = speciesRepository.getAllSpeciesName()

    //badges

    private suspend fun insertBadge(context:Context ,name: String, userId: Int) {
        val badges = receivedRepository.getByUser(userId).firstOrNull() ?: emptyList()
        val badgeNames = badges.map { it.name }
        if (name !in badgeNames) {
            receivedRepository.insert(Received(name, userId))
            Notifications.sendNotification(
                name,
                context.getString(R.string.badge_notification_text),
                "Badge Received")
        }
    }

    fun getLastDate(activity: String, pet: Pet): LocalDate {
        return if(activity == FEEDING){
            feedingRepository.getLastFeedingDate(pet.petId) ?: pet.birthday
        }else {
            cleaningRepository.getLastCleaningDate(pet.petId) ?: pet.birthday
        }
    }

    fun getSpecieDetails(scientificName: String) = speciesRepository.getByName(scientificName)
}