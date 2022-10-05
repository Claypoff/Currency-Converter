import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class CurrencyConverter {
    final String BASE_CURRENCY = "USD";
    String apiUrl = "https://api.exchangerate.host/";
    DecimalFormat f = new DecimalFormat("#.##");

    Connection conn;
    Map<String, Object> symbolsMap;
    boolean usingTable = false;

    public static void main(String[] args) {
        // Create instance of CurrencyConverter class and run the main program
        CurrencyConverter currencyConverter = new CurrencyConverter();
        currencyConverter.run();
    }

    // This is requested public function that returns the converted currency amount
    public double ConvertCurrency(String currencyFrom, String currencyTo, double amount) {
        double ratio = getConvertedCurrency(currencyFrom, currencyTo);
        return (amount * ratio);
    }

    private void run()  {
        // get all the available symbols and connect to the database
        getSymbols();
        connect();

        // create scanner and run user menu
        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to the Currency Converter!\n");

        boolean isPlaying = true;
        while (isPlaying) {
            System.out.println("Please select an option:");
            System.out.println("    1: convert USD amount to any currency (uses datatable)");
            System.out.println("    2: convert a currency to another specific currency (uses API)");
            System.out.println("    3: print out list of available currency codes");
            System.out.println("    4: quit program");

            // Verify that the user's input was a number, or else set it to -1
            int selection = sc.hasNextInt() ? sc.nextInt() : -1;
            sc.nextLine();
            switch (selection) {
                case 1:
                    usingTable = true;
                    convertUSDToCurrency(sc);
                    break;
                case 2:
                    usingTable = false;
                    convertToAnyCurrency(sc);
                    break;
                case 3:
                    printValidSymbols();
                    break;
                case 4:
                    isPlaying = false;
                    break;
                default:
                    System.out.println("Invalid selection! Please enter a valid selection\n");
            }
        }

        closeConnection();
        System.out.println("\nThank you for using the Currency Converter!");
    }

    /*
    Selection option methods
    */
    private void convertUSDToCurrency(Scanner sc) {
        System.out.println("\nPlease enter the 3 letter currency code you would like to convert USD to:");
        String currency = sc.nextLine().toUpperCase();

        while (isNotValidSymbol(currency)) {
            System.out.println("Invalid currency code!");
            System.out.println("Please enter the 3 letter currency code you would like to convert USD to:");
            currency = sc.nextLine().toUpperCase();
        }

        System.out.println("Please enter the amount of USD you would like to convert to " + currency + ":");
        double amount = sc.nextDouble();

        double total = ConvertCurrency(BASE_CURRENCY, currency, amount);
        System.out.println("your converted amount of "  + String.valueOf(amount) + " USD is " + f.format(total) + " " + currency + "\n");
    }

    private void convertToAnyCurrency(Scanner sc) {
        System.out.println("\nPlease enter the 3 letter currency code you would like to convert FROM:");
        String currencyFrom = sc.nextLine().toUpperCase();

        while (isNotValidSymbol(currencyFrom)) {
            System.out.println("Invalid currency code!");
            System.out.println("Please enter the 3 letter currency code you would like to convert FROM:");
            currencyFrom = sc.nextLine().toUpperCase();
        }

        System.out.println("Please enter the 3 letter currency code you would like to convert TO:");
        String currencyTo = sc.nextLine().toUpperCase();

        while (isNotValidSymbol(currencyTo)) {
            System.out.println("Invalid currency code!");
            System.out.println("Please enter the 3 letter currency code you would like to convert TO:");
            currencyTo = sc.nextLine().toUpperCase();
        }

        System.out.println("Please enter the amount of " + currencyFrom + " you would like to convert to " + currencyTo + ":");
        double amount = sc.nextDouble();

        double total = ConvertCurrency(currencyFrom, currencyTo, amount);
        System.out.println("your converted amount of " + String.valueOf(amount) + " " + currencyFrom + " is " + f.format(total) + " " + currencyTo + "\n");
    }

    private void printValidSymbols() {
        // Print out all the currency codes and their descriptions
        System.out.println("\nHere is a list of all valid currency codes:");
        symbolsMap.forEach( (key, value) ->
                System.out.println("    " + key + ": " + ((Map) value).get("description"))
        );
        System.out.print("\n");
    }

    private boolean isNotValidSymbol(String currencyCode) {
        return !symbolsMap.containsKey(currencyCode);
    }


    /*
    Currency Conversion Methods
    */
    private double getConvertedCurrency(String from, String to) {
        // Use boolean to know if user wants to use the table or api to convert currency
        return usingTable ? convertWithTable(to) : convertWithAPI(from, to);
    }

    private double convertWithTable(String to) {
        String sql = "SELECT ExchangeRate FROM usd_conversion_rates WHERE CurrencyCode = ?";

        try (PreparedStatement pstmt  = conn.prepareStatement(sql)) {
            pstmt.setString(1,to);

            ResultSet rs  = pstmt.executeQuery();
            return rs.getDouble("ExchangeRate");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private double convertWithAPI(String from, String to) {
        try {
            URL url = new URL(apiUrl + "convert?from=" + from + "&to=" + to);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();

            JsonElement response = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent()));
            return response.getAsJsonObject().get("result").getAsDouble();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("API error using api to get converted currency. Please rerun program.");
            throw new RuntimeException(e);
        }
    }


    /*
    Connection Methods
    */
    private void connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:currency-conversion.sqlite";
        conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    void closeConnection() {
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    Get list of all currency codes and make a Hashmap for them
    */
    private void getSymbols() {
        try {
            URL url = new URL(apiUrl + "/symbols");
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();

            JsonElement response = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent()));
            JsonObject req_result = response.getAsJsonObject().get("symbols").getAsJsonObject();

            symbolsMap = new Gson().fromJson(req_result, new TypeToken<HashMap<String, Object>>() {}.getType());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}