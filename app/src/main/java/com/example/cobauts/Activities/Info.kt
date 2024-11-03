package com.example.cobauts.Activities

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.cobauts.R

class Info : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info) // Ganti dengan layout yang sesuai

        // Misalkan kamu memiliki tombol di layout ini yang akan membuka popup
        val popupBTN: Button = findViewById(R.id.btn_popup) // ID tombol yang sesuai

        // Set OnClickListener untuk membuka dialog popup
        popupBTN.setOnClickListener {
            openPopup()
        }
    }

    private fun openPopup() {
        val mDialog = Dialog(this)
        mDialog.setContentView(R.layout.activity_popup) // Pastikan ini sesuai dengan layout popup yang kamu buat

        // Mengatur tombol tutup di dialog
        val closeButton = mDialog.findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            mDialog.dismiss() // Menutup dialog
        }

        mDialog.show() // Menampilkan dialog
    }
}