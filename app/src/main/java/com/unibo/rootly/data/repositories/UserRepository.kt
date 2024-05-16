package com.unibo.rootly.data.repositories

import android.content.ContentResolver
import android.net.Uri
import com.unibo.rootly.data.database.User
import com.unibo.rootly.data.database.daos.UserDao
import com.unibo.rootly.utils.saveImageToStorage

class UserRepository(
    private val userDao: UserDao,
    private val contentResolver: ContentResolver
) {
    suspend fun insert(user: User) :Long = userDao.insertUser(user)

    fun getUserByUsername(username: String) = userDao.getUserByUsername(username)

    fun getUserById(userId: Int): User? = userDao.getUserById(userId)

    fun setProPic(id: Int, uri: Uri) {
        val imageUri = saveImageToStorage(
            uri,
            contentResolver,
            "ProfilePic${id}"
        )
        userDao.setProPic(id, imageUri.toString())
    }
}