package org.projects.shoppinglist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity implements MyDialogFragment.OnPositiveListener {


    ArrayAdapter<Product> adapter;
    ListView listView;
    ArrayList<Product> bag = new ArrayList<Product>();

    FirebaseListAdapter<Product> fbadapter;

    public FirebaseListAdapter getMyFBAdapter() { return fbadapter; }

    // metode for at få specifikt produkt
    public Product getItem(int index) {
        return (Product) getMyFBAdapter().getItem(index);
    }

    public ArrayAdapter getMyAdapter()
    {
        return adapter;
    }

    // bruges til dialog fragment
    static MyDialogFragment dialog;
    static Context context;
    int spinnerPosition = 0;

    // til snackbar - bruges til at holde styr på de sidst slettede produkter
    Product lastDeletedProduct;
    int lastDeletedPosition;
    String lastDeletedKey;


    // database reference
    private DatabaseReference firebase;

    // firebase remote config
    private FirebaseRemoteConfig myFirebaseRemoteConfig;

    private FirebaseAuth mAuth;
    FirebaseUser user;



    // bruges til at gemme en kopi af tidligere produkter - som evt. slettes og vil trækkes tilbage
    public void saveCopy()
    {
        // det sidste slettede produkts position
        lastDeletedPosition = listView.getCheckedItemPosition();

        // det sidst slettede produkt
        lastDeletedProduct = getItem(lastDeletedPosition); // var bag.get

        // får fat i adapteren for firebase, finder referencen dertil for den sidste slettede position og får fat i key'en
        // denne key holder styr på hvor produktet var før, da den fungerer som en slags time-stamp
        lastDeletedKey = getMyFBAdapter().getRef(lastDeletedPosition).getKey();
    }

    @Override
    public void onPositiveClicked() {
        // Her kommer al funktionaliteten fra Clear knappen, da det kun skal
        // udføres, når man klikker "Ja" ved Dialog

                // viser toast om at man har cleared listen for at informere brugeren
                Toast toastClear = Toast.makeText(context,
                        "You have cleared the list", Toast.LENGTH_LONG);
                toastClear.show();

                // TODO - skal slettes senere
                // clear listen af varer
                //bag.clear();

                // sletter alt fra databasen - ville ikke virke med .child("items"), så derfor blev det på denne måde
                // wiper firebase databasen, kan man sige
                firebase.setValue(null);

                // skal huske at have denne her, så ændringen vises for brugeren
                //getMyAdapter().notifyDataSetChanged();
                getMyFBAdapter().notifyDataSetChanged();
    }



    public void setup()
    {


        // Login - authentication og user
        this.mAuth = FirebaseAuth.getInstance();
        // får fat i den nuværende bruger via authentication
        this.user = mAuth.getCurrentUser();

        // hvis brugeren er logget ind, skal databasen udfyldes med brugerens ID og så er items som child ved brugerens ID
        // dette er sådan så brugere har hver sin shoppingliste og altså ikke deler den samme
        if(user != null) {
            // firebase reference - får fat i items for en bestemt bruger
            firebase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("items");

        } else {
            // firebase reference - får fat i "default" items, hvis man ikke er logget ind
            firebase = FirebaseDatabase.getInstance().getReference().child("items");
        }

        // subklasse af FirebaseListAdapter - af typen Product
        fbadapter = new FirebaseListAdapter<Product>(this, Product.class, android.R.layout.simple_list_item_checked, firebase)
        {
            // overrider populateView metoden fra FirebaseListAdapter klassen
            // metoden kaldes for hvert produkt, der findes i databasen
            // passer Product og et view
            @Override
            protected void populateView (View view, Product product,int i){
                TextView textView = (TextView) view.findViewById(android.R.id.text1); //standard android id.
                textView.setText(product.toString());
            }
        };

        listView = (ListView) findViewById(R.id.list);

        // sætter adapteren til listview'et
        listView.setAdapter(fbadapter);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_main);

        //setIntent(MyPreferenceFragment.getName(this));

        // kører mine metoder, der viser brugerens indtastede navn og status
        showUsersName();
        showUserStatus();

        setup();
        // får fat i listview via det id det fik i xml filen


        // Får fat i Remote Config instance
        myFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // får fat i app-name som default - for at kunne bruge det til at ændre det i Firebase console
        Map<String,Object> defaults = new HashMap<>();
        defaults.put("app_name", getResources().getString(R.string.app_name));
        myFirebaseRemoteConfig.setDefaults(defaults);

        // sætter firebase remote config indstillinger
        FirebaseRemoteConfigSettings configSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        // sættes til true, da den ellers er meget langsom?
                        .setDeveloperModeEnabled(true)  //set to false when releasing
                        .build();

        myFirebaseRemoteConfig.setConfigSettings(configSettings);

        // sættes til et milisekund, så det sker hurtigst muligt
        Task<Void> myTask = myFirebaseRemoteConfig.fetch(1);

        // listener på den task, der gør det muligt at bruge de config ændringer, der kan være opstået efter de er loadet
        // ændrer navnet på app'en i Actionbaren til "Remember your groceries"
        myTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    myFirebaseRemoteConfig.activateFetched();
                    String name = myFirebaseRemoteConfig.getString("app_name");
                    getSupportActionBar().setTitle(name);

                    FirebaseCrash.log("app name from server:"+name);
                    FirebaseCrash.report(new Exception("Logging"));
                } else
                    Log.d("ERROR","Task not successful + "+task.getException());
            }
        });





        // TODO - slettes senere, da jeg ikke ville have spinner alligevel
//        getActionBar().setHomeButtonEnabled(true); // så man kan klikke på app'ens navn/home

        //The spinner is defined in our xml file
        //Spinner spinner = (Spinner) findViewById(R.id.spinner);


        // får adgang til vores gemte data, bruger ArrayList, som er det bag er
        // tjekker om der er gemt noget
        if (savedInstanceState != null) {
            ArrayList<Product> saved = savedInstanceState.getParcelableArrayList("bagSaved"); // i stedet for getStringArrayList

            // hvis der er gemt noget
            if (saved != null) {
                bag = saved;
            }
        } //else {
            // hvis ikke der er gemt noget, viser den bare default
           // bag.add("2 Bananas");
           // bag.add("4 Apples");
        //}

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // TODO - slettes senere
        // opretter en adapter til spinneren
        /*
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                this, R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);
        */


        //here we create a new adapter linking the bag and the
        //listview
       // adapter =  new ArrayAdapter<Product>(this,
                //android.R.layout.simple_list_item_checked,bag );

        //setting the adapter on the listview
        //listView.setAdapter(adapter);

        // sætter choicemode - her kan man kun vælge ét produkt af gangen
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // gemmer det tjekkede item - måske ikke alligevel
        //listView.getCheckedItemPosition();


        // click listener på "add" knappen
        Button addButton = (Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // får fat i edittext feltet
                EditText editText = (EditText) findViewById(R.id.textfield);
                EditText editText1 = (EditText) findViewById(R.id.quantityfield);
                //Spinner spinner1 = (Spinner) findViewById(R.id.spinner); - valgte ikke at have spinner

                // laver en string, som får fat i edit text og konverterer det til en string
               // String productName = editText.getText().toString();

                //String productName = "";
                int quantityAmount = 0;

                // TODO - udkommenterer alt det kode jeg ikke bruger

                // da dette er blevet til en int, bruges Integer.parseInt for at konvertere den til en string
                //int quantityAmount = Integer.parseInt(editText1.getText().toString());

//                String productNameString = "";
//                String quantityAmountString = editText1.getText().toString();
                //quantityAmountString = "";

                //String productName = "";

                // får ikke app'en til at crashe, når man ikke har indtastet et tal i Quantity edittext feltet
                // den prøver at få quantity amout til at blive til en string og opsnappe det indtastede
                // den fanger her NumberFormatException fejlen, som opstår hvis edittext feltet ikke er udfyldt (så den ikke crasher)
                // hvis denne fejl fanges, så laver jeg en toast, der gør brugeren opmærksom på at feltet ikke er udfyldt
                // fandt hjælp her (til fremtidig reference) - http://stackoverflow.com/questions/35613416/android-app-crashes-when-edittext-is-blank-and-button-pressed
                try {
                    // da dette er blevet til en int (via Product klassen), bruges Integer.parseInt for at konvertere den til en string
                    quantityAmount = Integer.parseInt(editText1.getText().toString());
                }
                catch(NumberFormatException ex) {
                    // Advarer brugeren om at feltet for Quantity ikke er udfyldt
                    Toast.makeText(MainActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();

                    return;
                }

                String productName = editText.getText().toString();


                // tjekker om Product edittext feltet er tomt, hvis det er skal brugeren gøres opmærksom på det skal udfyldes (via toast)
                // hvis det IKKE er tomt, skal det indtastede sættes i productName
                // fandt hjælp her (til fremtidig reference) - http://stackoverflow.com/questions/10782042/android-program-crashing-when-edittext-has-nothing-in-it
                if (productName.equals(null) || productName.equals("") || productName.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a product", Toast.LENGTH_SHORT).show();
                } else {
                    // tilføjer quantityAmount og productName til listen
                    bag.add(new Product(productName, quantityAmount));


                    // tilføjer et produkt til listen
                    Product product = new Product(productName, quantityAmount);

                    // tilføjer produktet til firebase databasen
                    firebase.push().setValue(product);


                    // getMyAdapter().notifyDataSetChanged();
                    // siger til Listview at data er ændret - at jeg altså har tilføjet ting
                    getMyFBAdapter().notifyDataSetChanged();

                    // reference til Firebase crash reporting - rapporterer en "non-fatal" fejl, der skriver "Product added" i loggen
                    FirebaseCrash.report(new Exception("Non-fatal error"));
                    FirebaseCrash.log("Product added");
                }
//                } else {
//                    // laver en string, som får fat i edit text og konverterer det til en string
//                    productName = editText.getText().toString();
//                }



                // productName.equals("")
                // hvis det er en if og else, så kommer den aldrig ned i else fordi productName er tom
//                if(productName.isEmpty()) {
//                    Toast.makeText(MainActivity.this, "Please enter a product", Toast.LENGTH_SHORT).show();
//
//                }
//
//                    productName = editText.getText().toString();

//                try {
//                    productName = editText.getText().toString();
//                }
//                catch(NumberFormatException ex) {
//                    //They didn't enter a number.  Pop up a toast or warn them in some other way
//                    Toast.makeText(MainActivity.this, "Please enter a product", Toast.LENGTH_SHORT).show();
//                    return;
//                }



//                if(editText != null) {
//                    productName = editText.getText().toString();
//                } else {
//                    Toast.makeText(MainActivity.this, "Please enter a product", Toast.LENGTH_SHORT).show();
//                }


//                if(quantityAmountString.length() == 0 || quantityAmountString.isEmpty()) {

//                if(quantityAmountString == "") {
//                    Toast.makeText(MainActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();
//                } else {
//                    quantityAmount = Integer.parseInt(quantityAmountString);
//                }

//                if(editText1 != null) {
//                    quantityAmount = Integer.parseInt(quantityAmountString);
//
//                } else {
//                    Toast.makeText(MainActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();
//                }







                // har værdien fra spinneren - valgte ikke at have spinner
                //String spinnerText = spinner1.getSelectedItem().toString();

                // Google - edittext equal spinner value - valgte ikke at have spinner
                /*if (editText1 == null) {
                    // hvis quantityAmount er tom, så skal den tage værdien fra spinneren

                    // har værdien fra spinneren
                    String spinnerText = spinner1.getSelectedItem().toString();

                    quantityAmount = Integer.parseInt(spinnerText);



                    // String spinnerAmount = (String) spinner1.getSelectedItem();
                   // Integer.parseInt(spinnerAmount) = quantityAmount;
                } */

               // if (!productName.isEmpty()) {


               // }
            }
        });

        // finder slet knappen og sætter click listener på den
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // gemmer en kopi af den slettede vare
                saveCopy();

                // sletter den valgte vare
                //bag.remove(lastDeletedPosition);

                // får fat i det specifikke produkt og sletter det fra databasen - sletter ved at sætte value til null
                int index = listView.getCheckedItemPosition();

                getMyFBAdapter().getRef(index).setValue(null);



                // viser ændringen for brugeren
                getMyFBAdapter().notifyDataSetChanged();

                // laver snackbar så hvis brugeren kommer til at slette noget og fortryder
                // så har brugeren mulighed for at undo
                final View parent = listView;
                Snackbar snackbar = Snackbar
                        .make(parent, "Deleted the product", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                // TODO - skal slettes senere
                                //getItem(index); //index
                                //bag.add(lastDeletedPosition,lastDeletedProduct);
                                //getMyAdapter().notifyDataSetChanged();
                                //firebase.push().setValue(lastDeletedProduct);

                                // får fat i child item fra databasen (som er et produkt) og her findes den sidst slettede key
                                // denne key holder styr på hvor produktet var før, da den fungerer som en slags time-stamp
                                // sætter value til at være det sidst slettede produkt - da det der, vi gerne vil have ind igen ved UNDO
                                firebase.child(lastDeletedKey).setValue(lastDeletedProduct);
                                getMyFBAdapter().notifyDataSetChanged();
                                Snackbar snackbar = Snackbar.make(parent, "Product has been restored, yay!", Snackbar.LENGTH_SHORT);

                                // ændrer farven på snackbarens tekst til hvid
                                View sbView = snackbar.getView();
                                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                                textView.setTextColor(Color.WHITE);

                                // viser snakbaren
                                snackbar.show();
                            }
                        });

                // ændrer farven på UNDO
                snackbar.setActionTextColor(Color.YELLOW);

                // ændrer farven på snackbarens tekst til hvid
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE);

                // viser snakbaren
                snackbar.show();
                getMyFBAdapter().notifyDataSetChanged();

                // TODO - skal slettes senere!
                //getMyAdapter().notifyDataSetChanged();


                // sætter den tjekkede vare/værdi i en int
               // int checked = listView.getCheckedItemPosition();


                // sletter den valgte vare
                //bag.remove(checked);

                // skal huske at have denne her, så ændringen vises for brugeren
                //getMyAdapter().notifyDataSetChanged();
            }
        }); // click listener for delete knap slutter

        // finder clear knappen

        // TODO - skal slettes, bruges ikke mere
        /*Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // funktionaliteten køres fra onPositiveClickListener
                showDialog();

            }
        });*/



       // Spinner spinner1 = (Spinner) findViewById(R.id.spinner);
        //final String spinnerAmount = (String) spinner1.getSelectedItem();

        // listener på spinner
        /*
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //The AdapterView<?> type means that this can be any type,
            //so we can use both AdapterView<String> or any other


            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                //So this code is called when ever the spinner is clicked
                spinnerPosition = position;
                Toast.makeText(MainActivity.this,
                        "Item selected: " + spinnerPosition, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO you would normally do something here
                // for instace setting the selected item to "null"
                // or something.
            }
        });
        */

        //add some stuff to the list so we have something
        // to show on app startup
        // udkommenteret, ellers kommer der nye frem efter hver drejning af mobilen
        //bag.add("2 Bananas");
        //bag.add("4 Apples");

    }

    // bruger denne metode til at vise navnet på brugeren fra mit Preference fragment
    private void showUsersName() {

        // navnet sættes til at være det brugeren har indtastet via User setting
        String name = MyPreferenceFragment.getName(this);

        // toasten der skal udskrives - som er en velkomst besked til brugeren
        String message = "Welcome back "+name+"";
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    // bruges til at vise brugerens status - om brugeren har tid til at handle ind eller ej
    private void showUserStatus() {

        boolean isBusy = MyPreferenceFragment.isBusy(this);

        if (isBusy) {
            String message = "You don't have time to shop today";
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            toast.show();
        } else {
            String message = "You have time to shop today";
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            toast.show();
        }

    }


    // denne metode får fat i resultater via intents, hvor bl.a. UserSetting Activity bliver startet med startActiviryForResult og det samme
    // gælder for EmailPasswordActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) // betyder vi er kommet tilbage fra User settings
        {
            // Kalder metoden, der udskriver det indtastede navn i en toast
            showUsersName();

            // kalder metoden, der udskriver om bruger er busy eller ej
            showUserStatus();

        } else if (requestCode == 2) { // betyder at vi er kommet tilbage fra EmailPasswordActivity

            // laver her en toast for at vise brugeren at han/hun er logget ind og viser brugerens mail via user fra authentication
            // kører setup metoden for at få opdateret brugeren
            setup();
           if (user != null) {
               Toast toast = Toast.makeText(context, "Logged in as " + user.getEmail(), Toast.LENGTH_SHORT);
               toast.show();
           }

            // evt. ændre brugeren i userSettings, but how? - TODO

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //return true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.item_clear) {
            //showDialog();
            return true;
        }*/

        // switch statement, tjekker hvilket ikon fra actionbaren eller overflow, der er klikket på
        switch (item.getItemId()) {
            case android.R.id.home:
                return true; //return true, means we have handled the event
            case R.id.action_settings:
                return true;
            // hvis man klikker på X ikonet i actionbaren, så køres metoden, der før blev kørt ved tryk
            // på "Clear" knappen
            case R.id.item_clear:
                showDialog();
                return true;
            case R.id.item_user_settings:
                //Start our settingsactivity and listen to result - i.e.
                //when it is finished.
                Intent intent = new Intent(this,UserSettingsActivity.class);
                startActivityForResult(intent,1);
                //notice the 1 here - this is the code we then listen for in the
                //onActivityResult
                return true;
            case R.id.item_login_settings:
                // prøver at skifte til login acvitity
                Intent loginIntent = new Intent(this, EmailPasswordActivity.class);
                startActivityForResult(loginIntent, 2);
                // var startActivity - TODO prøver at få fat i brugeren

                return true;
            case R.id.item_share_list:
                // laver en instans af Product
                Product product;
                // laver en string - bruges til at opsamle produkterne som string
                String listItems = "";

                // gennemløber for loop for at få fat i alle produkter
                for (int it = 0; it < getMyFBAdapter().getCount(); it++) {
                    product = getItem(it);
                    if (product != null) {
                        listItems += product.toString() + "\n";
                    }
                }

                // Laver et intent for at kunne dele shoppinglisten - som tekst
                // https://developer.android.com/training/sharing/send.html
                Intent shareListIntent = new Intent();
                shareListIntent.setAction(Intent.ACTION_SEND);
                shareListIntent.putExtra(Intent.EXTRA_SUBJECT, "ShoppingList");
                shareListIntent.putExtra(Intent.EXTRA_TEXT, "I need to buy: \n" + listItems);
                shareListIntent.setType("text/plain");
                //startActivity(shareListIntent);
                // giver brugeren mulighed for at vælge imellem flere apps fremfor blot at tage "Meddelelser"
                startActivity(Intent.createChooser(shareListIntent, getResources().getText(R.string.shareTo)));
                return true;

            // som default skal den bare gøre det normale
            default:
                return super.onOptionsItemSelected(item);
        }

        //return false; //we did not handle the event

        //return super.onOptionsItemSelected(item);
    }

    // denne metode bliver kaldt, før activity bliver destroyed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // bør altid kalde super metoden
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("bagSaved", bag); // i stedet for putStringArrayList
    }


    // Denne metode køres, hvis brugeren trykker på X ikonet i actionbaren
    public void showDialog() {
        // viser Dialog

        dialog = new MyDialog();
        //Here we show the dialog
        //The tag "MyFragement" is not important for us.
        dialog.show(getFragmentManager(), "MyFragment");
    }

    public static class MyDialog extends MyDialogFragment {

        @Override
        protected void negativeClick() {
            // Denne metode køres, hvis brugeren trykker "No" til dialogen
            Toast toast = Toast.makeText(context,
                    "You chose not to clear", Toast.LENGTH_SHORT);
            toast.show();
        }
    }







}




