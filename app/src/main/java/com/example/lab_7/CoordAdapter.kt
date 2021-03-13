package com.example.lab_7

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class CoordAdapter(context: Context, coords: ArrayList<Coordinate>):
        BaseAdapter() {
    var ctx: Context = context
    var objects: ArrayList<Coordinate> = coords
    var inflater: LayoutInflater = ctx.
    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    // Формирование разметки, содержащей строку данных
    override fun getView(position: Int, convertView: View?,
                         parent: ViewGroup?): View {
        // Если разметка ещё не существует, создаём её по шаблону
        var view = convertView
        if (view == null)
            view = inflater.inflate(R.layout.listview_layout_coord,
                    parent, false)
        // Получение объекта с информацией о продукте
        val s = objects[position]
        // Заполнение элементов данными из объекта
        var v = view?.findViewById(R.id.coord_name) as TextView
        v.text = s.name
        v = view.findViewById(R.id.coord_lat_and_lon) as TextView
        val resText = ctx.resources.getString(R.string.coord_lat_and_lon)
        v.text = "Широта: ${s.lat.format(6)}; Долгота ${s.lon.format(6)}"
        return view
    }
    // Получение элемента данных в указанной строке
    override fun getItem(position: Int): Any {
        return objects[position]
    }
    // Получение идентификатора элемента в указанной строке
    // Часто вместо него возвращается позиция элемента
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    // Получение количества элементов в списке
    override fun getCount(): Int {
        return objects.size
    }
}