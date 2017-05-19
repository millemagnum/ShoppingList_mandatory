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


    ListView listView;
    ArrayList<Product> bag = new ArrayList<Product>();

    // laver Firebase Liste Adapter - før brugte vi en almindelig arrayAdapter, men nu da databasen er koblet på, bruges Firebases adapter
    FirebaseListAdapter<Product> fbadapter;

    public FirebaseListAdapter getMyFBAdapter() { return fbadapter; }

    // metode for at få specifikt produkt
    public Product getItem(int index) {
        return (Product) getMyFBAdapter().getItem(index);
    }

    // bruges til dialog fragment
    static MyDialogFragment dialog;
    static Context context;

    // til snackbar - bruges til at holde styr på de sidst slettede produkter
    Product lastDeletedProduct;
    int lastDeletedPosition;
    String lastDeletedKey;


    // database reference
    private DatabaseReference firebase;

    // firebase remote config
    private FirebaseRemoteConfig myFirebaseRemoteConfig;

    // Firebase authentication og user
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
        // Her kommer al funktionaliteten fra Clear knappen, da det kun skal udføres, når man klikker "Ja" ved Dialog

                // viser toast om at man har cleared listen for at informere brugeren
                Toast toastClear = Toast.makeText(context,
                        "You have cleared the list", Toast.LENGTH_LONG);
                toastClear.show();

                // sletter alt fra databasen - ville ikke virke med .child("items"), så derfor blev det på denne måde
                // wiper firebase databasen, kan man sige
                firebase.setValue(null);

                // skal huske at have denne her, så ændringen vises for brugeren
                getMyFBAdapter().notifyDataSetChanged();
    }


    // denne metode køres i onCreate og i onActivityResult (for at app'en ikke crasher, når man er logget ind og går tilbage til shoppinglisten)
    // ved at denne køres igen nede i onActivityResult, ved den hvem brugeren er
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

        // får fat i listview via det id det fik i xml filen
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

        // kører setup metoden, hvor user bl.a. findes og listen fetches fra databasen
        setup();

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

        // får adgang til vores gemte data, bruger ArrayList, som er det bag er
        // tjekker om der er gemt noget
        if (savedInstanceState != null) {
            ArrayList<Product> saved = savedInstanceState.getParcelableArrayList("bagSaved"); // i stedet for getStringArrayList

            // hvis der er gemt noget
            if (saved != null) {
                bag = saved;
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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

                // får fat i edittext felterne
                EditText editText = (EditText) findViewById(R.id.textfield);
                EditText editText1 = (EditText) findViewById(R.id.quantityfield);

                int quantityAmount = 0;

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

                // laver en string, som får fat i edit text og konverterer det til en string
                String productName = editText.getText().toString();


                // tjekker om Product edittext feltet er tomt, hvis det er skal brugeren gøres opmærksom på det skal udfyldes (via toast)
                // hvis det IKKE er tomt, skal det indtastede sættes i productName
                // fandt hjælp her (til fremtidig reference) - http://stackoverflow.com/questions/10782042/android-program-crashing-when-edittext-has-nothing-in-it
                if (productName.equals(null) || productName.equals("") || productName.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a product", Toast.LENGTH_SHORT).show();
                } else { // hvis produkt-feltet IKKE er tomt, så gemmes det i databasen og tilføjes til listen

                    // tilføjer quantityAmount og productName til listen
                    bag.add(new Product(productName, quantityAmount));

                    // tilføjer et produkt til listen
                    Product product = new Product(productName, quantityAmount);

                    // tilføjer produktet til firebase databasen
                    firebase.push().setValue(product);

                    // siger til Listview at data er ændret - at jeg altså har tilføjet ting
                    getMyFBAdapter().notifyDataSetChanged();

                    // reference til Firebase crash reporting - rapporterer en "non-fatal" fejl, der skriver "Product added" i loggen
                    // for at få afprøvet det
                    FirebaseCrash.report(new Exception("Non-fatal error"));
                    FirebaseCrash.log("Product added");
                }

            }
        });

        // finder slet knappen og sætter click listener på den
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // gemmer en kopi af den slettede vare
                saveCopy();

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

            }
        }); // click listener for delete knap slutter

    } // onCreate slutter

    // bruger denne metode til at vise navnet på brugeren fra MyPreferenceFragment
    private void showUsersName() {

        // navnet sættes til at være det brugeren har indtastet via User Settings
        String name = MyPreferenceFragment.getName(this);

        // toasten der skal udskrives - som er en velkomst besked til brugeren
        String message = "Welcome back "+name+"";
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    } //showUserName slutter

    // bruges til at vise brugerens status - om brugeren har tid til at handle ind eller ej
    private void showUserStatus() {

        // busy sættes til at være det brugeren har valgt i User Settings
        boolean isBusy = MyPreferenceFragment.isBusy(this);

        // hvis brugeren har vinget "busy" af i User Settings, skrives en toast om at brugeren ikke har tid til at handle ind på dagen
        if (isBusy) {
            String message = "You don't have time to shop today";
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            toast.show();
        } else { // hvis brugeren ikke har vinget busy af, kommer toast om at brugeren har tid til at handle ind på dagen
            String message = "You have time to shop today";
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            toast.show();
        }

    } // showUserStatus slutter


    // denne metode får fat i resultater via intents, hvor bl.a. UserSetting Activity bliver startet med startActiviryForResult og det samme
    // gælder for EmailPasswordActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) // betyder vi er kommet tilbage fra UserSettingsActivity
        {
            // Kalder metoden, der udskriver det indtastede navn i en toast
            showUsersName();

            // kalder metoden, der udskriver om bruger er busy eller ej
            showUserStatus();

        } else if (requestCode == 2) { // betyder at vi er kommet tilbage fra EmailPasswordActivity

            // laver her en toast for at vise brugeren at han/hun er logget ind og viser brugerens mail via user fra authentication
            // kører setup metoden for at få opdateret brugeren
            setup();

            // hvis brugeren ikke er null, skrives en toast, hvor brugeren bliver gjort opmærksom på at han/hun er logget ind som xx email
           if (user != null) {
               Toast toast = Toast.makeText(context, "Logged in as " + user.getEmail(), Toast.LENGTH_SHORT);
               toast.show();
           }

        }
        super.onActivityResult(requestCode, resultCode, data);
    } // onActivityResult slutter

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //return true;
        return super.onCreateOptionsMenu(menu);
    } // onCreateOptionsMenu slutter

    // her er metoden, hvor jeg håndterer actionbar/overflow clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // switch statement, tjekker hvilket ikon fra actionbaren eller overflow, der er klikket på
        switch (item.getItemId()) {
            case android.R.id.home:
                return true; // true betyder at eventet er håndteret
            // hvis man klikker på X ikonet i actionbaren, så køres metoden, der før blev kørt ved tryk på "Clear" knappen
            case R.id.item_clear:
                showDialog();
                return true;
            // user settings i overflow
            case R.id.item_user_settings:
                // starter UserSettingsActivity
                Intent intent = new Intent(this,UserSettingsActivity.class);
                // koden 1 er den jeg lytter efter i onActivityResult, hvor jeg reagerer på hvad der skal gøres, når brugeren kommer tilbage fra
                // UserSettingsActivity
                startActivityForResult(intent, 1);
                return true;
            // login i overflow - dette er Authentication med Email og Password
            case R.id.item_login_settings:
                // starter EmailPasswordActivity
                Intent loginIntent = new Intent(this, EmailPasswordActivity.class);
                // koden 2 lytter jeg efter i onActivityResult
                startActivityForResult(loginIntent, 2);
                return true;
            // dele-funktionen for shoppinglisten som text - i overflow
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

                // Laver et intent for at kunne dele shoppinglisten - som tekst - https://developer.android.com/training/sharing/send.html
                Intent shareListIntent = new Intent();

                // indikerer at intent sender data fra en activity til en anden
                shareListIntent.setAction(Intent.ACTION_SEND);

                // dette tilføjer et "emne", som kan bruges, hvis man vil sende listen som en mail
                shareListIntent.putExtra(Intent.EXTRA_SUBJECT, "ShoppingList");

                // tilføjer tekst før selve listen og så med hvert produkt på en ny linje, så det ser overskueligt ud, der hvor man deler det
                shareListIntent.putExtra(Intent.EXTRA_TEXT, "I need to buy: \n" + listItems);

                // sættes til text type
                shareListIntent.setType("text/plain");

                // giver brugeren mulighed for at vælge imellem flere apps fremfor blot at tage "Meddelelser"/SMS
                startActivity(Intent.createChooser(shareListIntent, getResources().getText(R.string.shareTo)));
                return true;

            // som default skal den bare gøre det normale
            default:
                return super.onOptionsItemSelected(item);
        }

    } // onOptionsItemSelected

    // denne metode bliver kaldt, før activity bliver destroyed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // bør altid kalde super metoden
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("bagSaved", bag); // i stedet for putStringArrayList
    } // onSaveInstanceState


    // Denne metode køres, hvis brugeren trykker på X ikonet i actionbaren - som clear listen
    public void showDialog() {

        // laver en ny MyDialog
        dialog = new MyDialog();

        // viser Dialog via Fragment Manager
        dialog.show(getFragmentManager(), "MyFragment");
    } // showDialog slutter

    public static class MyDialog extends MyDialogFragment {

        @Override
        protected void negativeClick() {
            // Denne metode køres, hvis brugeren trykker "No" til dialogen - altså vælger ikke at clear listen
            Toast toast = Toast.makeText(context,
                    "You chose not to clear", Toast.LENGTH_SHORT);
            toast.show();
        }
    } // MyDialog slutter


} // MainActivity slutter




