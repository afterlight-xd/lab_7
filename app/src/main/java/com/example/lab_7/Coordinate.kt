package com.example.lab_7

import android.os.Parcel
import android.os.Parcelable

class Coordinate() : Parcelable {
    var id: Int = -1
    var name: String = ""
    var lon: Double = 0.0 //долгота
    var lat: Double = 0.0 //широта

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        name = parcel.readString() ?: ""
        lon = parcel.readDouble()
        lat = parcel.readDouble()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        if (dest !== null) {
            dest.writeInt(id)
            dest.writeString(name)
            dest.writeDouble(lon)
            dest.writeDouble(lat)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Coordinate> {
        override fun createFromParcel(parcel: Parcel): Coordinate {
            return Coordinate(parcel)
        }

        override fun newArray(size: Int): Array<Coordinate?> {
            return arrayOfNulls(size)
        }
    }
}