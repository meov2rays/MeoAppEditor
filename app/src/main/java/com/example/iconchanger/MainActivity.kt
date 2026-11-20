package com.example.iconchanger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

class MainActivity : Activity() {
    private val reqPick = 1
    private var picked: Bitmap? = null
    private var preview: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val list = ListView(this)
        list.adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(i: Int) = apps[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, p: ViewGroup?): View {
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(32, 24, 32, 24)
                }
                row.addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(apps[i].loadIcon(pm))
                    layoutParams = LinearLayout.LayoutParams(120, 120)
                })
                row.addView(TextView(this@MainActivity).apply {
                    text = apps[i].loadLabel(pm)
                    textSize = 18f
                    setPadding(32, 0, 0, 0)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, 120, 1f)
                })
                return row
            }
        }
        list.setOnItemClickListener { _, _, i, _ -> showDialog(apps[i]) }
        setContentView(list)
    }

    private fun showDialog(app: ResolveInfo) {
        val pm = packageManager
        picked = null
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(this).apply {
            setText(app.loadLabel(pm)); hint = "اسم جدید"
        }
        preview = ImageView(this).apply {
            setImageDrawable(app.loadIcon(pm))
            layoutParams = LinearLayout.LayoutParams(220, 220)
        }
        val pickBtn = Button(this).apply {
            text = "انتخاب لوگو از گالری"
            setOnClickListener {
                startActivityForResult(
                    Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), reqPick
                )
            }
        }
        box.addView(nameInput); box.addView(preview); box.addView(pickBtn)

        AlertDialog.Builder(this)
            .setTitle(app.loadLabel(pm))
            .setView(box)
            .setPositiveButton("ساخت شورتکات") { _, _ ->
                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(app.activityInfo.packageName, app.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val bmp = picked ?: app.loadIcon(pm).toBitmap(192, 192)
                val label = nameInput.text.toString().ifBlank { app.loadLabel(pm).toString() }
                val info = ShortcutInfoCompat.Builder(this, "s_" + System.currentTimeMillis())
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(IconCompat.createWithBitmap(bmp))
                    .setIntent(launch)
                    .build()
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                    ShortcutManagerCompat.requestPinShortcut(this, info, null)
                } else {
                    Toast.makeText(this, "لانچر شما پشتیبانی نمی‌کند", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqPick && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)?.let { b ->
                    picked = Bitmap.createScaledBitmap(b, 192, 192, true)
                    preview?.setImageBitmap(picked)
                }
            }
        }
    }
}
