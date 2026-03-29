package com.osprey74.michinavi.model

enum class Feature(val key: String, val label: String) {
    ATM("atm", "ATM"),
    RESTAURANT("restaurant", "レストラン"),
    ONSEN("onsen", "温泉"),
    EV_CHARGER("ev_charger", "EV充電"),
    WIFI("wifi", "Wi-Fi"),
    BABY_ROOM("baby_room", "授乳室"),
    DISABLED_TOILET("disabled_toilet", "障害者トイレ"),
    INFORMATION("information", "情報コーナー"),
    SHOP("shop", "物販"),
    EXPERIENCE("experience", "体験施設"),
    MUSEUM("museum", "資料館"),
    PARK("park", "公園"),
    HOTEL("hotel", "宿泊"),
    RV_PARK("rv_park", "RVパーク"),
    DOG_RUN("dog_run", "ドッグラン"),
    BICYCLE_RENTAL("bicycle_rental", "レンタサイクル"),
    CAMPING("camping", "キャンプ"),
    FOOTBATH("footbath", "足湯");

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun fromKey(key: String): Feature? = byKey[key]
    }
}
