package de.dralle.bluetoothtest.BGS;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nils on 22.06.16.
 */
public class Util {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = Util.class.getName();

    private static Util ourInstance = new Util();

    private Util() {
    }

    public static Util getInstance() {
        return ourInstance;
    }

    /**
     * If a string contains multiple JSON objects, this method will seperate them
     *
     * @param json
     * @return
     */
    public List<JSONObject> splitJSON(String json) {
        List<JSONObject> jsoS = new ArrayList<>();
        String rest = json;
        int braces = 0;
        int prevJSONEnd = 0;
        for (int i = 0; i < json.length(); i++) {
            if (json.charAt(i) == '{') {
                braces++;
            } else if (json.charAt(i) == '}') {
                braces--;
            }
            if (braces == 0) {
                String nestedJSON = json.substring(prevJSONEnd, i + 1);
                try {
                    jsoS.add(new JSONObject(nestedJSON));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                prevJSONEnd = i + 1;
            }
        }
        return jsoS;
    }
}
