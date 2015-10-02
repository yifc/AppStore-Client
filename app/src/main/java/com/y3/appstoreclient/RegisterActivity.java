package com.y3.appstoreclient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yifei.appstoreclient.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class RegisterActivity extends Activity {
    public static final String GET_URL = "http://10.1.11.33/y3/elgg-1.8.20/services/api/rest/xml/";
    private Button register;
    private EditText nameT, usernameT, passwordT, emailT;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
    }

    private void initView(){
        register = (Button) findViewById(R.id.button1);
        nameT = (EditText) findViewById(R.id.editText1);
        emailT = (EditText) findViewById(R.id.editText2);
        usernameT = (EditText) findViewById(R.id.editText3);
        passwordT = (EditText) findViewById(R.id.editText4);
        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String username = usernameT.getText().toString();
                String password = passwordT.getText().toString();
                String name = nameT.getText().toString();
                String email = emailT.getText().toString();
                Boolean allow_multiple_emails = false;
                int friend_guid = 0;
                String invitecode = "";
                try {
                    register(username, password, name, email, allow_multiple_emails, friend_guid, invitecode);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }


    private boolean register(String username, String password, String name, String email,
                             Boolean allow_multiple_emails, int friend_guid, String invitecode)
            throws UnsupportedEncodingException{
        String loginUrl = GET_URL + "?method=register&username=" + username + "&password=" +
                password + "&name=" + URLEncoder.encode(name, "utf-8") + "&email=" + email +
                "&allow_multiple_emails=false&friend_guid=0&invitecode=10";
        new MyAsyncTask().execute(loginUrl);
        return true;
    }

    class MyAsyncTask extends AsyncTask<String, Integer, String> {

        // <summary>
        // Both register and login will use this thread execute function
        // </summary>
        // <param name="params[0]">this parameter is url which refers the restful API</param>
        // <returns></returns>
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
                if(status.equals("-1"))
                {
                    result = eElement.getElementsByTagName("message").item(0).getTextContent();
                }
                if(status.equals("0"))
                {
                    result = eElement.getElementsByTagName("result").item(0).getTextContent();
                }
                System.out.println("status : " + status);
                System.out.println("result : " + result);
                Pattern pattern = Pattern.compile("[0-9]*");
                if(status.equals("0")){
                    //If the result is digit it means that register successfully, then login automatically
                    if (pattern.matcher(result).matches()){
                        login(usernameT.getText().toString(),passwordT.getText().toString());
                    }
                    //If the result is true, it means that login successfully, then go to the user home page
                    else if (result.equals("true")){
                        Intent intent = new Intent();
                        intent.setClass(RegisterActivity.this, UserActivity.class);
                        intent.putExtra("username", usernameT.getText().toString());
                        startActivity(intent);
                    }
                    //If the result is none of above, then display the error message.

//					Intent intent = new Intent();
//					intent.setClass(RegisterActivity.this, UserActivity.class);
//					intent.putExtra("id", result);
//					startActivity(intent);
                }
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
        @Override
        protected void onPostExecute(String result){
            if(!result.equals("true")&&!result.matches("[0-9]*")){
                CharSequence text = result;
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                toast.show();
            }
        }
    }

    private boolean login(String username, String password){
        String loginUrl = GET_URL + "?method=login&username=" + username + "&password=" + password + "&persistent=false";
        try{
            new MyAsyncTask().execute(loginUrl);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }
}