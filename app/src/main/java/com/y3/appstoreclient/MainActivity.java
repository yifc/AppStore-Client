package com.y3.appstoreclient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yifei.appstoreclient.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

//import android.app.ActionBarActivity;


public class MainActivity extends Activity {

    private Button loginBtn;
    private Button registerBtn;
    private TextView message;
    private EditText userName;
    private EditText passWord;
    public static final String GET_URL = "http://10.1.11.33/y3/elgg-1.8.20/services/api/rest/xml/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView(){
        loginBtn = (Button) findViewById(R.id.button1);
        registerBtn = (Button) findViewById(R.id.button2);
        userName = (EditText) findViewById(R.id.editText1);
        passWord = (EditText) findViewById(R.id.editText2);
        loginBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                login(userName.getText().toString(),passWord.getText().toString());
            }
        });
        registerBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // <summary>
    // In this function we will create a new thread which execute the login
    // </summary>
    // <param name="username">The user login name</param>
    // <param name="password">The user password</param>
    // <returns>Boolean result which represent failure or success</returns>
    private boolean login(String username, String password){
        String loginUrl = GET_URL + "?method=login&username=" + username + "&password=" +
                password + "&persistent=false";
        try{
            new MyAsyncTask().execute(loginUrl);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }



    class MyAsyncTask extends AsyncTask<String, Integer, String> {

        // <summary>
        // This function run in new thread.
        // In this function, we firstly call the restful API of login with given URL.
        // Secondly, we parse the xml result which is returned by Http request.
        // </summary>
        // <param name="params[0]">The URL refer the restful API</param>
        // <returns>The result of login</returns>
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            URL UrlObject = null;
            String status = null;
            String result = null;
            try {
                UrlObject = new URL(params[0]);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                // Send Http request to call the restful API remotely.
                HttpURLConnection connection = (HttpURLConnection) UrlObject.openConnection();
                InputStream in = new BufferedInputStream(connection.getInputStream());

                // Parse the XML Result
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(in);
                doc.getDocumentElement().normalize();

                System.out.println("--------------------------------------------------------");
                System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
                System.out.println("--------------------------------------------------------");
                Element eElement = doc.getDocumentElement();
                status = eElement.getElementsByTagName("status").item(0).getTextContent();
                result = eElement.getElementsByTagName("result").item(0).getTextContent();
                System.out.println("status : " + status);
                System.out.println("result : " + result);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return result;
        }

        // <summary>
        // This function run after the doInBackground function automatically .
        // In this function, we judge according to the result. If the result is true,
        // we go to the user home page. If login fail, the error message will be display.
        // </summary>
        // <param name="result">This parameter is passed from doInBackground return value</param>
        // <returns>void</returns>
        @Override
        protected void onPostExecute(String result){
            if(result.equals("true")){
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, UserActivity.class);
                intent.putExtra("username", userName.getText().toString());
                startActivity(intent);
                finish();
            }
            else{
                //message = (TextView)findViewById(R.id.textView4);
                //message.setText(result);
                CharSequence text = "We could not log you in. Please check your username/email and password.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                toast.show();
            }
        }

    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
