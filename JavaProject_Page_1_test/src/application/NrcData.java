package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.HashMap;
import java.util.Map;

public class NrcData {

    public static final ObservableList<String> STATES = FXCollections.observableArrayList(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"
    );

    public static final ObservableList<String> TYPES = FXCollections.observableArrayList(
        "(N)", "(P)", "(E)", "(A)", "(F)", "(M)"
    );

    private static final Map<String, ObservableList<String>> TOWNSHIP_MAP = new HashMap<>();

    static {
        // 1. Kachin State
        TOWNSHIP_MAP.put("1", FXCollections.observableArrayList("MyiKyina", "BaMoNa", "MaHnyiNa", "PuTaOo", "Hpakan", "MoGaung", "ShweKu", "Waingmaw"));
        // 2. Kayah State
        TOWNSHIP_MAP.put("2", FXCollections.observableArrayList("LaYaNa", "BaLaNa", "PhaSaNa", "MoBwaNa", "YaTaNa", "DaMaSa", "Hpruso"));
        // 3. Kayin State
        TOWNSHIP_MAP.put("3", FXCollections.observableArrayList("PaAnNa", "KaKaRe", "MyaWaHta", "Hpapun", "Kawkareik", "Thandaunggyi", "KyaInSeikgyi"));
        // 4. Chin State
        TOWNSHIP_MAP.put("4", FXCollections.observableArrayList("FaLaNa", "HaKhaNa", "MaInTa", "Kanpetlet", "Matupi", "Mindat", "Tonzang", "Tiddim", "Paletwa"));
        // 5. Sagaing Region
        TOWNSHIP_MAP.put("5", FXCollections.observableArrayList("MaNyaNa", "SaBaNa", "SaGaNa", "KaLaNa", "KaThaNa", "Hkamti", "Kalay", "Katha", "Khin-U", "Kyunhla", "Mawlaik", "Monywa", "Pale", "Shwebo", "Taze", "Wuntho", "Ye-U"));
        // 6. Tanintharyi Region
        TOWNSHIP_MAP.put("6", FXCollections.observableArrayList("DaWaYa", "MaMyaNa", "KaThaNa", "Bokpyin", "Dawei", "Kyunsu", "Myeik", "Kawthaung", "Palaw", "Thayetchaung"));
        // 7. Bago Region
        TOWNSHIP_MAP.put("7", FXCollections.observableArrayList("BaGaLa", "PaYinNa", "ThaRaWaNa", "PaKhaNa", "Bago", "Daik-U", "Kawa", "Letpadan", "Minhla", "Monyo", "Nattalin", "Okpo", "Pyay", "Taungoo", "Tharrawaddy", "Waw", "Zigon"));
        // 8. Magway Region
        TOWNSHIP_MAP.put("8", FXCollections.observableArrayList("MaGaNa", "MaMaNa", "PaKaKu", "ThaYaNa", "GaGaNa", "Aunglan", "Chauk", "Gangaw", "Kamma", "Magway", "Minbu", "Myothit", "Natmauk", "Ngape", "Pakokku", "Pauk", "Pwintbyu", "Sagu", "Seikphyu", "Sidoktaya", "Sinbaungwe", "Thayet"));
        // 9. Mandalay Region
        TOWNSHIP_MAP.put("9", FXCollections.observableArrayList("MaHaMyiNa", "PaAnNa", "NyaUNa", "Mandalay", "PyinOoLwin", "Meiktila", "Kyaukse", "Yamethin", "MaTaNa", "Amarapura", "Aungmyethazan", "Chanmyathazi", "Chanayechantha", "Kyaukpadaung", "Mahaaungmye", "Myingyan", "Natogyi", "Ngazun", "Patheingyi", "Pyawbwe", "Singu", "Tada-U", "Thabeikkyin", "Wundwin"));
        // 10. Mon State
        TOWNSHIP_MAP.put("10", FXCollections.observableArrayList("MaLaMyaNa", "ThaTaNa", "KaTaLa", "Bilin", "Chaungsone", "Kyaikto", "Mawlamyine", "Mudon", "Paung", "Thaton"));
        // 11. Rakhine State
        TOWNSHIP_MAP.put("11", FXCollections.observableArrayList("SaTaNa", "MaNaTa", "KaPaNa", "ThaNaWa", "Ann", "Buthidaung", "Gwa", "Kyaukpyu", "Maungdaw", "Minbya", "Mrauk-U", "Myebon", "Pauktaw", "Ponnagyun", "Ramree", "Rathedaung", "Sittwe", "Thandwe", "Toungup"));
        // 12. Yangon Region
        TOWNSHIP_MAP.put("12", FXCollections.observableArrayList("BaHNa", "LaMaTa", "KaMaYa", "SaGaNa", "DaGaNa", "AhBeNa", "BaBaNa", "TaTaNa", "AhSaNa", "BoTaTaung", "Cocokyun", "Dagon", "Dagon Myothit (East)", "Dagon Myothit (North)", "Dagon Myothit (Seikkan)", "Dagon Myothit (South)", "Hlaing", "Hlaingthaya", "Hlegu", "Hmawbi", "Htantabin", "Insein", "Kamayut", "Kayan", "Kawhmu", "Kyauktada", "Kyauktan", "Kunjwan", "Mingalar Taungnyunt", "North Okkalapa", "Pazundaung", "Sanchaung", "Seikgyikanaungto", "Seikkan", "Shwepyitha", "South Okkalapa", "Tamwe", "Thaketa", "Thingangyun", "Yankin"));
        // 13. Shan State
        TOWNSHIP_MAP.put("13", FXCollections.observableArrayList("TaKaNa", "LaSaNa", "KaTaNa", "MuSeNa", "Ayadaw", "Hopong", "Hsipaw", "Kalaw", "Kengtung", "Kunhing", "Kyaukme", "Lashio", "Loilem", "Monghsat", "Mongkaung", "Mongnai", "Mongpan", "Mongton", "Namhsan", "Namtu", "Nansang", "Nyangshwe", "Pindaya", "Pinlaung", "Taunggyi"));
        // 14. Ayeyarwady Region
        TOWNSHIP_MAP.put("14", FXCollections.observableArrayList("PaThain", "Hinthada", "Myaungmya", "Maubin", "Pyapon", "Bogale", "Danubyu", "Dedaye", "Kyaiklat", "Kyangin", "Ingapu", "Labutta", "Mawlamyinegyun", "Myanaung", "Ngapudaw", "Ngathaingchaung", "Wakema", "Zalun"));
    }

    public static ObservableList<String> getTownshipsForState(String stateCode) {
        if (stateCode == null) return FXCollections.observableArrayList();
        return TOWNSHIP_MAP.getOrDefault(stateCode.trim(), FXCollections.observableArrayList());
    }

    public static ObservableList<String> getAllTownships() {
        ObservableList<String> all = FXCollections.observableArrayList();
        for (ObservableList<String> list : TOWNSHIP_MAP.values()) {
            for (String t : list) {
                if (!all.contains(t)) {
                    all.add(t);
                }
            }
        }
        FXCollections.sort(all);
        return all;
    }

    public static String getStateForTownship(String township) {
        if (township == null) return null;
        for (Map.Entry<String, ObservableList<String>> entry : TOWNSHIP_MAP.entrySet()) {
            if (entry.getValue().contains(township.trim())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String getCodeForTownship(String township) {
        if (township == null) return "111111";
        return "111111";
    }
}