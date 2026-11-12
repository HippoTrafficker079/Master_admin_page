package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

/**
 * Shared NRC entry logic for Add and Modify student forms. Requires only JavaFX.
 * Keeps the existing Student.nrc/database value as a single String.
 */
public final class NrcFieldSupport {
    private static final Pattern NRC_PATTERN = Pattern.compile(
            "^\\s*(1[0-4]|[1-9])\\s*/\\s*([\\p{L}\\p{M}]+)\\s*\\(\\s*([\\p{L}\\p{M}]+)\\s*\\)\\s*([0-9]{6})\\s*$");
    private static final List<String> NRC_TYPES = Arrays.asList("N", "P", "E", "A", "T", "Y", "S");
    private static final Map<String, Map<String, String>> TOWNSHIPS_BY_STATE = loadTownships();

    private final ComboBox<String> stateCombo;
    private final ComboBox<String> townshipCombo;
    private final ComboBox<String> typeCombo;
    private final TextField numberField;
    private final ObservableList<String> townships = FXCollections.observableArrayList();
    private final FilteredList<String> visibleTownships = new FilteredList<>(townships, code -> true);
    private final Set<String> legacyCodes = new HashSet<>();
    private boolean settingFields;
    private String unparsedLegacyNrc;

    public NrcFieldSupport(ComboBox<String> state, ComboBox<String> township,
                           ComboBox<String> type, TextField number) {
        if (state == null || township == null || type == null || number == null) {
            throw new IllegalStateException("Missing NRC fx:id; check Add/Modify FXML files.");
        }
        this.stateCombo = state;
        this.townshipCombo = township;
        this.typeCombo = type;
        this.numberField = number;
        init();
    }

    private void init() {
        List<String> states = new ArrayList<>();
        for (int i = 1; i <= 14; i++) states.add(String.valueOf(i));
        stateCombo.setItems(FXCollections.observableArrayList(states));
        typeCombo.setItems(FXCollections.observableArrayList(NRC_TYPES));

        townshipCombo.setEditable(true);
        townshipCombo.setItems(visibleTownships);
        townshipCombo.setDisable(true);
        townshipCombo.setVisibleRowCount(12);

        townshipCombo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);
                setText(empty || code == null ? null : code + "  -  " + townshipName(code));
            }
        });

        numberField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9]{0,6}") ? change : null));

        stateCombo.valueProperty().addListener((obs, oldState, newState) -> {
            if (!settingFields) legacyCodes.clear();
            refreshTownships();
        });

        townshipCombo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (settingFields || !townshipCombo.getEditor().isFocused()) return;
            String query = newText == null ? "" : newText.trim().toLowerCase(Locale.ROOT);
            visibleTownships.setPredicate(code -> code.toLowerCase(Locale.ROOT).contains(query)
                    || townshipName(code).toLowerCase(Locale.ROOT).contains(query));
            if (!townshipCombo.isShowing() && !townshipCombo.isDisabled()) {
                Platform.runLater(() -> {
                    if (townshipCombo.getEditor().isFocused() && !townshipCombo.isShowing()) {
                        townshipCombo.show();
                    }
                });
            }
        });

        townshipCombo.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !settingFields) commitTownshipEditor();
        });
    }

    private void refreshTownships() {
        settingFields = true;
        try {
            String state = stateCombo.getValue();
            townshipCombo.setValue(null);
            townshipCombo.getEditor().clear();
            visibleTownships.setPredicate(code -> true);
            List<String> codes = new ArrayList<>(TOWNSHIPS_BY_STATE
                    .getOrDefault(state, Map.of()).keySet());
            codes.sort(String::compareTo);
            townships.setAll(codes);
            townshipCombo.setDisable(state == null || state.isBlank());
        } finally {
            settingFields = false;
        }
    }

    private void commitTownshipEditor() {
        String entered = normalizeCode(townshipCombo.getEditor().getText());
        String matched = townships.stream()
                .filter(code -> code.equalsIgnoreCase(entered))
                .findFirst().orElse(null);
        settingFields = true;
        try {
            visibleTownships.setPredicate(code -> true);
            townshipCombo.setValue(matched);
            if (matched == null) townshipCombo.getEditor().setText(entered);
        } finally {
            settingFields = false;
        }
    }

    /** Formats a complete selection to e.g. 12/KAMAYA(N)123456. An entirely empty NRC stays optional. */
    public String getNrc() {
        commitTownshipEditor();
        String state = stateCombo.getValue();
        String code = normalizeCode(townshipCombo.getValue());
        String type = typeCombo.getValue();
        String digits = numberField.getText() == null ? "" : numberField.getText().trim();
        String pending = townshipCombo.getEditor().getText().trim();

        if (isBlank(state) && isBlank(code) && isBlank(type) && digits.isEmpty() && pending.isEmpty()) {
            return unparsedLegacyNrc == null ? "" : unparsedLegacyNrc;
        }
        if (isBlank(state)) throw new IllegalArgumentException("Please select an NRC state/region code (1-14).");
        if (isBlank(code) || !townships.contains(code)) {
            throw new IllegalArgumentException("Please select a valid township code from the searchable dropdown.");
        }
        if (isBlank(type) || !NRC_TYPES.contains(type)) {
            throw new IllegalArgumentException("Please select an NRC type (e.g. N, P or E).");
        }
        if (!digits.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("NRC serial number must contain exactly six digits (leading zeros allowed).");
        }
        return state + "/" + code + "(" + type + ")" + digits;
    }

    /** Loads a saved NRC from the existing Student.nrc field into four controls. */
    public void setNrc(String nrc) {
        clear();
        if (isBlank(nrc) || nrc.trim().equalsIgnoreCase("null")) return;
        Matcher match = NRC_PATTERN.matcher(nrc.trim());
        if (!match.matches()) {
            unparsedLegacyNrc = nrc.trim();
            stateCombo.setTooltip(new Tooltip("Existing NRC (needs review): " + nrc));
            numberField.setPromptText("Review NRC");
            return;
        }
        String state = match.group(1);
        String township = normalizeCode(match.group(2));
        String type = normalizeType(match.group(3));
        String serial = match.group(4);

        settingFields = true;
        try {
            stateCombo.setValue(state);
            if (!townships.contains(township)) {
                townships.add(township);
                legacyCodes.add(township);
                FXCollections.sort(townships);
            }
            visibleTownships.setPredicate(code -> true);
            townshipCombo.setValue(township);
            townshipCombo.getEditor().setText(township);
            if (NRC_TYPES.contains(type)) typeCombo.setValue(type);
            numberField.setText(serial);
        } finally {
            settingFields = false;
        }
    }

    public void clear() {
        settingFields = true;
        try {
            legacyCodes.clear();
            unparsedLegacyNrc = null;
            stateCombo.setTooltip(null);
            numberField.setPromptText("6 digits");
            stateCombo.setValue(null);
            townshipCombo.setValue(null);
            townshipCombo.getEditor().clear();
            typeCombo.setValue(null);
            numberField.clear();
            townships.clear();
            visibleTownships.setPredicate(code -> true);
            townshipCombo.setDisable(true);
        } finally {
            settingFields = false;
        }
    }

    private String townshipName(String code) {
        return TOWNSHIPS_BY_STATE.getOrDefault(stateCombo.getValue(), Map.of())
                .getOrDefault(code, legacyCodes.contains(code) ? "Existing code (verify)" : code);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.strip().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeType(String type) {
        if (type == null) return "";
        String trimmed = type.trim();
        switch (trimmed) {
            case "နိုင်": return "N";
            case "ဧည့်":
            case "ဧည့်": return "E";
            case "ပြု": return "P";
            case "စ":
            case "စီ": return "S";
            default: return trimmed.toUpperCase(Locale.ROOT);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static Map<String, Map<String, String>> loadTownships() {
        Map<String, Map<String, String>> map = new HashMap<>();
        try (InputStream in = NrcFieldSupport.class.getResourceAsStream("nrc_townships.csv")) {
            if (in == null) {
                throw new IllegalStateException("Missing nrc_townships.csv in the application package.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#") || line.startsWith("state_code")) continue;
                    String[] columns = line.split(",", -1);
                    if (columns.length < 4) continue;
                    String state = columns[0].trim();
                    String code = normalizeCode(columns[1]);
                    String name = columns[3].trim();
                    if (!state.matches("1[0-4]|[1-9]") || !code.matches("[A-Z]+")) continue;
                    map.computeIfAbsent(state, key -> new LinkedHashMap<>()).putIfAbsent(code, name);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load NRC township codes", ex);
        }
        return map;
    }
}
