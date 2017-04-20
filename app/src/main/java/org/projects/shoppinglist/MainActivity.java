package org.projects.shoppinglist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

public class MainActivity extends AppCompatActivity implements MyDialogFragment.OnPositiveListener {


    ArrayAdapter<Product> adapter;
    ListView listView;
    ArrayList<Product> bag = new ArrayList<Product>();

    public ArrayAdapter getMyAdapter()
    {
        return adapter;
    }

    // bruges til dialog fragment
    static MyDialogFragment dialog;
    static Context context;
    int spinnerPosition = 0;

    // til snackbar
    Product lastDeletedProduct;
    int lastDeletedPosition;

    public void saveCopy()
    {
        lastDeletedPosition = listView.getCheckedItemPosition();
        lastDeletedProduct = bag.get(lastDeletedPosition);
    }

    @Override
    public void onPositiveClicked() {
        // Her kommer al funktionaliteten fra Clear knappen, da det kun skal
        // udføres, når man klikker "Ja" ved Dialog

                // viser toast om at man har cleared listen for at informere brugeren
                Toast toastClear = Toast.makeText(context,
                        "You have cleared the list", Toast.LENGTH_LONG);
                toastClear.show();

                // clear listen af varer
                bag.clear();

                // skal huske at have denne her, så ændringen vises for brugeren
                getMyAdapter().notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_main);

        //setIntent(MyPreferenceFragment.getName(this));

        showUsersName();
        showUserStatus();

//        getActionBar().setHomeButtonEnabled(true); // så man kan klikke på app'ens navn/home

        //The spinner is defined in our xml file
        //Spinner spinner = (Spinner) findViewById(R.id.spinner);


        // får adgang til vores gemte data, bruger ArrayList som er det bag er
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

        // opretter en adapter til spinneren
        /*
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                this, R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);
        */

        //getting our listiew - you can check the ID in the xml to see that it
        //is indeed specified as "list"
        listView = (ListView) findViewById(R.id.list);
        //here we create a new adapter linking the bag and the
        //listview
        adapter =  new ArrayAdapter<Product>(this,
                android.R.layout.simple_list_item_checked,bag );

        //setting the adapter on the listview
        listView.setAdapter(adapter);
        //here we set the choice mode - meaning in this case we can
        //only select one item at a time.
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // gemmer det tjekkede item - måske ikke alligevel
        //listView.getCheckedItemPosition();




        Button addButton = (Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // får fat i edittext feltet
                EditText editText = (EditText) findViewById(R.id.textfield);
                EditText editText1 = (EditText) findViewById(R.id.quantityfield);
                //Spinner spinner1 = (Spinner) findViewById(R.id.spinner);

                // laver en string, som får fat i edit text og konvertere det til en string
                String productName = editText.getText().toString();
                // da dette er blevet til en int, bruges Integer.parseInt for at konvertere den til en string

                int quantityAmount = Integer.parseInt(editText1.getText().toString());

                // har værdien fra spinneren
                //String spinnerText = spinner1.getSelectedItem().toString();

                // Google - edittext equal spinner value
                /*if (editText1 == null) {
                    // hvis quantityAmount er tom, så skal den tage værdien fra spinneren

                    // har værdien fra spinneren
                    String spinnerText = spinner1.getSelectedItem().toString();

                    quantityAmount = Integer.parseInt(spinnerText);



                    // String spinnerAmount = (String) spinner1.getSelectedItem();
                   // Integer.parseInt(spinnerAmount) = quantityAmount;
                } */


                // tilføjer quantityAmount og productName til listen
                bag.add(new Product(productName, quantityAmount));
                //The next line is needed in order to say to the ListView
                //that the data has changed - we have added stuff now!
                getMyAdapter().notifyDataSetChanged();
            }
        });

        // finder slet knappen
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // gemmer en kopi af den slettede vare
                saveCopy();

                // sletter den valgte vare
                bag.remove(lastDeletedPosition);

                // viser ændringen for brugeren
                getMyAdapter().notifyDataSetChanged();

                // laver snackbar så hvis brugeren kommer til at slette noget og fortryder
                // så har brugeren mulighed for at undo
                final View parent = listView;
                Snackbar snackbar = Snackbar
                        .make(parent, "Deleted product", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                bag.add(lastDeletedPosition,lastDeletedProduct);
                                getMyAdapter().notifyDataSetChanged();
                                Snackbar snackbar = Snackbar.make(parent, "Product restored!", Snackbar.LENGTH_SHORT);
                                snackbar.show();
                            }
                        });

                // ændrer farven på UNDO
                snackbar.setActionTextColor(Color.RED);

                // ændrer farven på snackbarens tekst - men gælder ikke den anden besked
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE);

                snackbar.show();
                getMyAdapter().notifyDataSetChanged();


                // sætter den tjekkede vare/værdi i en int
               // int checked = listView.getCheckedItemPosition();


                // sletter den valgte vare
                //bag.remove(checked);

                // skal huske at have denne her, så ændringen vises for brugeren
                //getMyAdapter().notifyDataSetChanged();
            }
        });

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
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
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



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==1) // betyder vi er kommet tilbage fra User settings
        {
            // Kalder metoden, der udskriver det indtastede navn i en toast
            showUsersName();

            // kalder metoden, der udskriver om bruger er busy eller ej
            showUserStatus();
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

        // switch statement, tjekker hvilket ikon fra actionbaren, der er klikket på
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




