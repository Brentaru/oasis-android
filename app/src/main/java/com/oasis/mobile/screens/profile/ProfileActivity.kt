package com.oasis.mobile.screens.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.browse.BrowseActivity
import com.oasis.mobile.screens.home.HomeActivity
import com.oasis.mobile.screens.landing.LandingActivity
import com.oasis.mobile.screens.library.LibraryActivity
import com.oasis.mobile.utils.BottomNavTab
import com.oasis.mobile.utils.app
import com.oasis.mobile.utils.getButtonView
import com.oasis.mobile.utils.getEditTextValue
import com.oasis.mobile.utils.setEditTextText
import com.oasis.mobile.utils.setupBottomNavIcons
import com.oasis.mobile.utils.start
import com.oasis.mobile.utils.toast

class ProfileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupBottomNav()
        findViewById<EditText>(R.id.edittextPassword).isEnabled = false

        app().getUserInfo().let {
            setEditTextText(R.id.edittextUsername, it.username)
            setEditTextText(R.id.edittextPassword, it.username)
            bindProfileHeader(it.firstname.ifBlank { it.username }, it.username, it.profilePhoto)
            findViewById<TextView>(R.id.textviewEmail).text = if (it.username.contains("@")) it.username else ""
        }

        loadProfile()

        val buttonEditSave = getButtonView(R.id.buttonEditSave)
        buttonEditSave.setOnClickListener {
            val displayName = getEditTextValue(R.id.edittextUsername)

            if (displayName.isNotEmpty()) {
                saveProfile(displayName)
            } else {
                toast("Display name is required")
            }
        }
        getButtonView(R.id.buttonChangePassword).setOnClickListener { changePassword() }
        getButtonView(R.id.buttonChangePhoto).setOnClickListener { choosePhoto() }
        getButtonView(R.id.buttonDeleteAccount).setOnClickListener { confirmDeleteAccount() }
    }

    private fun loadProfile() {
        val userInfo = app().getUserInfo()
        if (userInfo.userId.isBlank()) {
            return
        }

        Thread {
            try {
                val profile = OasisApiClient.getProfile(userInfo)
                runOnUiThread {
                    app().setUserInfo(profile)
                    setEditTextText(R.id.edittextUsername, profile.firstname.ifBlank { profile.username })
                    setEditTextText(R.id.edittextPassword, profile.username)
                    bindProfileHeader(profile.firstname.ifBlank { profile.username }, profile.username, profile.profilePhoto)
                }
            } catch (_: Exception) {
                runOnUiThread { toast("Profile is using local data for now") }
            }
        }.start()
    }

    private fun saveProfile(displayName: String) {
        val userInfo = app().getUserInfo()
        getButtonView(R.id.buttonEditSave).isEnabled = false

        Thread {
            try {
                if (userInfo.userId.isNotBlank()) {
                    OasisApiClient.updateProfile(userInfo, displayName)
                }
                val updated = userInfo.copy(firstname = displayName)
                app().setUserInfo(updated)
                runOnUiThread {
                    bindProfileHeader(displayName, updated.username, updated.profilePhoto)
                    toast("Profile saved")
                    getButtonView(R.id.buttonEditSave).isEnabled = true
                }
            } catch (_: Exception) {
                runOnUiThread {
                    toast("Unable to save profile")
                    getButtonView(R.id.buttonEditSave).isEnabled = true
                }
            }
        }.start()
    }

    private fun changePassword() {
        val current = getEditTextValue(R.id.edittextCurrentPassword)
        val next = getEditTextValue(R.id.edittextNewPassword)
        val confirm = getEditTextValue(R.id.edittextConfirmPassword)
        if (current.isBlank() || next.isBlank() || confirm.isBlank()) {
            toast("Password fields are required")
            return
        }

        getButtonView(R.id.buttonChangePassword).isEnabled = false
        Thread {
            try {
                OasisApiClient.changePassword(app().getUserInfo(), current, next, confirm)
                val updated = app().getUserInfo().copy(password = next)
                app().setUserInfo(updated)
                runOnUiThread {
                    setEditTextText(R.id.edittextCurrentPassword, "")
                    setEditTextText(R.id.edittextNewPassword, "")
                    setEditTextText(R.id.edittextConfirmPassword, "")
                    toast("Password changed")
                    getButtonView(R.id.buttonChangePassword).isEnabled = true
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    toast(exception.message ?: "Unable to change password")
                    getButtonView(R.id.buttonChangePassword).isEnabled = true
                }
            }
        }.start()
    }

    private fun choosePhoto() {
        startActivityForResult(Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }, REQUEST_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadPhoto(uri)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        val current = app().getUserInfo()
        val localPhoto = current.copy(profilePhoto = uri.toString())
        app().setUserInfo(localPhoto)
        bindProfileHeader(localPhoto.firstname.ifBlank { localPhoto.username }, localPhoto.username, localPhoto.profilePhoto)

        if (current.userId.isBlank()) {
            toast("Picture updated")
            return
        }

        Thread {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                val type = contentResolver.getType(uri) ?: "image/jpeg"
                val uploaded = OasisApiClient.uploadProfilePhoto(current, bytes, "profile.jpg", type).ifBlank { uri.toString() }
                val updated = app().getUserInfo().copy(profilePhoto = uploaded)
                app().setUserInfo(updated)
                runOnUiThread {
                    bindProfileHeader(updated.firstname.ifBlank { updated.username }, updated.username, updated.profilePhoto)
                    toast("Picture updated")
                }
            } catch (_: Exception) {
                runOnUiThread { toast("Picture saved on this phone") }
            }
        }.start()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Delete account")
            .setMessage("This will remove your account from Oasis.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .show()
    }

    private fun deleteAccount() {
        getButtonView(R.id.buttonDeleteAccount).isEnabled = false
        Thread {
            try {
                OasisApiClient.deleteAccount(app().getUserInfo())
                app().setUserInfo(com.oasis.mobile.data.UserInfo())
                runOnUiThread {
                    toast("Account deleted")
                    start(LandingActivity::class.java)
                    finish()
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    toast(exception.message ?: "Unable to delete account")
                    getButtonView(R.id.buttonDeleteAccount).isEnabled = true
                }
            }
        }.start()
    }

    private fun bindProfileHeader(displayName: String, email: String, photo: String) {
        findViewById<TextView>(R.id.textviewDisplayName).text = displayName.ifBlank { "Oasis Reader" }
        findViewById<TextView>(R.id.textviewEmail).text = if (email.contains("@")) email else ""
        findViewById<TextView>(R.id.textviewAvatar).text = displayName.ifBlank { email }.firstOrNull()?.toString()?.uppercase() ?: "O"
        if (photo.isBlank()) {
            findViewById<TextView>(R.id.textviewAvatar).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.imageviewProfilePhoto).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.textviewAvatar).visibility = View.GONE
            findViewById<ImageView>(R.id.imageviewProfilePhoto).apply {
                visibility = View.VISIBLE
                setProfilePhoto(photo)
            }
        }
    }

    private fun ImageView.setProfilePhoto(photo: String) {
        if (photo.startsWith("data:image")) {
            try {
                val encoded = photo.substringAfter(",", "")
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) {
                setImageURI(null)
            }
        } else {
            setImageURI(Uri.parse(photo))
        }
    }

    private fun setupBottomNav() {
        setupBottomNavIcons(BottomNavTab.PROFILE)
        findViewById<android.view.View>(R.id.navHome).setOnClickListener { start(HomeActivity::class.java) }
        findViewById<android.view.View>(R.id.navLibrary).setOnClickListener { start(LibraryActivity::class.java) }
        findViewById<android.view.View>(R.id.navBrowse).setOnClickListener { start(BrowseActivity::class.java) }
    }

    companion object {
        private const val REQUEST_PHOTO = 42
    }
}
