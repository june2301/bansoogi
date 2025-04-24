//package com.example.eggi.ui.simple
//
//import androidx.lifecycle.ViewModel
//import com.example.eggi.data.local.RealmProvider
//import com.example.eggi.data.model.Person
//
//class SimpleViewModel : ViewModel() {
//    fun saveData(person: Person) {
//        val realm = RealmProvider.realm
//
//        realm.writeBlocking {
//            copyToRealm(person)
//        }
//    }
//
//    fun loadData(): List<Person> {
//        val realm = RealmProvider.realm
//        return realm.query(Person::class).find()
//    }
//
//    // ViewModel 종료 시 Realm 닫기는 필요하지 않음
//    // 앱 전체에서 공유하는 인스턴스이므로 Application 종료 시에만 닫음
//}