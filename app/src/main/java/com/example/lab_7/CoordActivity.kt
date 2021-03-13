package com.example.lab_7

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class CoordActivity : AppCompatActivity(){
    private val RESULT_DELETE = 15
    private var index = 0
    private lateinit var coord: Coordinate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coord)

        val intent = intent
        index = intent?.getIntExtra("index", -1) ?: -1
        coord = intent?.getParcelableExtra("coord") ?: Coordinate()
        val editName = findViewById<EditText>(R.id.name)
        editName.setText(coord.name)
        val editLon = findViewById<EditText>(R.id.lon)
        editLon.setText(coord.lon.toString())
        val editLat = findViewById<EditText>(R.id.lat)
        editLat.setText(coord.lat.toString())

        // Включаем кнопку Назад
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_coord, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        if (item.itemId == R.id.action_save) {
            this.coord.name = findViewById<EditText>(R.id.name).
            text.toString()
            this.coord.lat = findViewById<EditText>(R.id.lat).
            text.toString().toDouble()
            this.coord.lon = findViewById<EditText>(R.id.lon).
            text.toString().toDouble()
            val intent = Intent()
            intent.putExtra("index", index)
            intent.putExtra("coord", this.coord)
            setResult(RESULT_OK, intent)

            finish()
            return true
        }

        if (item.itemId == R.id.action_delete) {
            val intent = Intent()
            intent.putExtra("coord to delete", this.coord)
            setResult(RESULT_DELETE, intent)

            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}