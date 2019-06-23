package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

//    static ArrayList<String> listOfFiles = new ArrayList<String>();
    static String TAG="";
    static String exactPort = "edu.buffalo.cse.cse486586.simpledht";
    static final int FINAL_PORT = 10000;
    static String smallest="";
    static String largest= "";
    static String encodedPort="";
    static HashMap<String, String> hmPort = new HashMap<String, String>();
    static  HashMap<String,String> newMap= new HashMap<String,String>();

    static ArrayList<String> ownDirectory = new ArrayList<String>();
    static ArrayList<String> hashedPortValues = new ArrayList<String>();
     Uri mUri= OnTestClickListener.mUri;
    static Node selfPortValue= new Node();


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //refernce: https://stackoverflow.com/questions/24659704/how-do-i-delete-files-programmatically-on-android
        if (selection.contains("@") || selection.contains("*")) {
            File[] ownDirectory = getContext().getFilesDir().listFiles();
            for (File obj : ownDirectory) {
                obj.delete();

            }
        }
        else{

            File direct =  getContext().getFilesDir();

            File obj= new File(direct,selection);
            if(obj.exists())
                obj.delete();
        }
//        else {
//
//            Log.e("deletepath", getContext().getFilesDir() + "..");
//            Log.e("delete", "file Deleted :" + uri.getPath());
//
//            File fdelete = new File(uri.getPath());
//
//            if (fdelete.exists()) {
//                if (fdelete.delete()) {
//                    Log.e("delete", "file Deleted :" + uri.getPath());
//                } else {
//                    Log.e("delete: ", "file not Deleted :" + uri.getPath());
//                }
//            }
//
//        }
        return 0;
    }


//reference: https://stackoverflow.com/questions/8152125/how-to-create-text-file-and-insert-data-to-that-file-on-android/8152217
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String msg_key = values.getAsString("key");
        String msg_value = values.getAsString("value");
        Context con = getContext();
        try {
            String predPortHashed=genHash(selfPortValue.getPrevPort());
            String succPortHashed=genHash(selfPortValue.getNextPort());
            String keyHashed=genHash(msg_key);

            if(selfPortValue.getNextPort().equals("") && selfPortValue.getPrevPort().equals("")) {
                FileOutputStream writer = con.openFileOutput(msg_key, Context.MODE_PRIVATE);
                writer.write(msg_value.getBytes());
                writer.close();
                ownDirectory.add(msg_key);
                return uri;
            }

             if(succPortHashed.compareTo(encodedPort)>0 && predPortHashed.compareTo(encodedPort)>0 && smallest.equals("")){
                 smallest=exactPort;
            }else if(succPortHashed.compareTo(encodedPort)<0 && predPortHashed.compareTo(encodedPort)<0 && largest.equals("")){
                 largest=exactPort;

             }
            if(!smallest.equals(exactPort) && keyHashed.compareTo(predPortHashed) > 0 && (keyHashed.compareTo(encodedPort) <= 0)) {


                    FileOutputStream writer = con.openFileOutput(msg_key, Context.MODE_PRIVATE);
                    writer.write(msg_value.getBytes());
                    writer.close();
                    ownDirectory.add(msg_key);

                    Log.e(TAG,msg_key);
                    return uri;


            }else  if(smallest.equals(exactPort) && ((keyHashed.compareTo(encodedPort)<=0) || ((keyHashed.compareTo(encodedPort)>0 && keyHashed.compareTo(predPortHashed)>0)))){
                    FileOutputStream writer = con.openFileOutput(msg_key, Context.MODE_PRIVATE);
                    writer.write(msg_value.getBytes());
                    writer.close();
                    ownDirectory.add(msg_key);

                    return uri;
                }
                else {
                    Log.e(TAG,"Checking if it entered the loop");
                    Node n = selfPortValue;
                    n.setType("sendToNext");
                    n.setMsgKey(msg_key);
                    n.setMsgValue(msg_value);
                    Log.e(TAG,"inside else loop: " +msg_key);
                ClientThread c = new ClientThread(n);
                    c.start();
                    return null;
                }




            } catch (Exception e) {
            Log.e(TAG, "exceptionHandling:" +e.getMessage());
        }
        return uri;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean onCreate() {
try {
    TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
    exactPort = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
    ServerThread s = new ServerThread();
    s.start();


    encodedPort = genHash(exactPort);
    selfPortValue = new Node();
    selfPortValue.setPrevPort("");
    selfPortValue.setNextPort("");
    Log.e(TAG, "Inside oncreate loop: " +exactPort + "");
    selfPortValue.setExactPort(exactPort);

    if (!exactPort.equals("5554")) {
        selfPortValue.setType("requestForJoin");
        try {
            ClientThread c = new ClientThread(selfPortValue);
            c.start();
        } catch (Exception e) {

        }
    } else {
        hmPort.put(encodedPort, selfPortValue.getExactPort());
        hashedPortValues.add(encodedPort);
    }

}
catch (Exception e){
    e.printStackTrace();
}
        return false;
    }

    @Override
    public  Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String contentvalues [] = {"key", "value"};
        MatrixCursor cursor = new MatrixCursor(contentvalues);
        Context context_obj = getContext();
        if( selection.equals("@")  ) {
            //fetch all files from own directory
            for (String s : ownDirectory) {
                try {
                    BufferedReader br=new BufferedReader(new InputStreamReader(context_obj.openFileInput(s)));
                    String query[] = {s, br.readLine()};
                    cursor.addRow(query);
                   br.close();
                } catch (IOException e) {
                    Log.e(TAG, "messages failed");
                }
            }
            return cursor;
        }
         else  if(selection.equals("*") && !selfPortValue.getPrevPort().equals("") && !selfPortValue.getNextPort().equals("")){
//select * performed at a particular avd in a ring with more than 1 avd
                Node n = selfPortValue;
                n.setType("requestedPortValues");
                ArrayList<String> arrayports= new ArrayList<String>();
                if(exactPort.equals("5554")){
                    for(Map.Entry<String,String> mp:hmPort.entrySet()){
                        arrayports.add(mp.getValue());
                    }
                }else {
                    ClientThread c = new ClientThread(n);
                    c.start();
                    arrayports = c.portsRetrieval();
                }
                for(String s:arrayports) {
                    Node node = selfPortValue;
                    node.setType("each");
                    node.setExactPort(s);
                    ClientThread c1 = new ClientThread(node);
                    c1.start();
                    ArrayList<String> allFromEachValues = c1.getAllFromEachPort();
                    if (allFromEachValues != null) {
                        for (String b : allFromEachValues) {
                            String query[] = {b.split(",")[0], b.split(",")[1]};
                            cursor.addRow(query);
                        }
                    }
                }
            }
            else if (selection.equals("*") && selfPortValue.getPrevPort().equals("") && selfPortValue.getNextPort().equals("")){
                //select * performed at single avd  when there is no other node in the ring other than itself
            for (String s : ownDirectory) {
                try {
                    BufferedReader br=new BufferedReader(new InputStreamReader(context_obj.openFileInput(s)));
                    String query[] = {s, br.readLine()};
                    cursor.addRow(query);
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, "messages failed");
                }
            }
            return cursor;
        }
        else {
            //looking for a particular key in its own directory of files
            if(ownDirectory.contains(selection)){
                try {
                    BufferedReader br=new BufferedReader(new InputStreamReader(context_obj.openFileInput(selection)));
                    String query[] = {selection, br.readLine()};
                    cursor.addRow(query);
                    br.close();
                    //return mc;
                } catch (IOException e) {
                    //Log.e(TAG,e.getMessage());
                    Log.e(TAG, "failed to get messages"+cursor.getColumnNames());
                }
            }
            //if key not found in it,cascade the request to its successor port
            else{
                try {
                    Log.e("Query",selection);
                    Node node= selfPortValue;
                    node.setMsgKey(selection);
                    node.setType("retreiving");
                    ClientThread c = new ClientThread(node);
                    c.start();
                    String[] valueRetrived=c.selectValueForKey();
                    cursor.addRow(valueRetrived);
                    return cursor;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        }

        return cursor;

    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }


    public class ServerThread extends Thread {
       public void run() {
            try {
                ServerSocket ss = new ServerSocket(FINAL_PORT);
                while (true) {
                    Socket socket = ss.accept();
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        Node serverNode = (Node) ois.readObject();
                        if (serverNode.getType().equals("requestForJoin")) {
                            String reqPort=serverNode.getExactPort();
                            String reqHashedValue = genHash(serverNode.getExactPort());
                            hmPort.put(reqHashedValue, serverNode.getExactPort());
                            hashedPortValues.add(reqHashedValue);
                            Collections.sort(hashedPortValues);
                            int size=hashedPortValues.size();
                            for (int num = 0; num <= size-1; num++) {
                                String s="";
                                if(num!=0 && num!=(size-1)){

                                    s=hmPort.get(hashedPortValues.get(num- 1))+","+hmPort.get(hashedPortValues.get(num+1));
                                    newMap.put(hmPort.get(hashedPortValues.get(num)), s);
                                }
                                else{
                                    if(num==0){

                                    s=hmPort.get(hashedPortValues.get(size- 1))+","+hmPort.get(hashedPortValues.get(1));
                                    newMap.put(hmPort.get(hashedPortValues.get(0)), s);
                                    }else{

                                    s=hmPort.get(hashedPortValues.get(num- 1))+","+hmPort.get(hashedPortValues.get(0));
                                    newMap.put(hmPort.get(hashedPortValues.get(size- 1)), s);
                                    }
                                }
                            }

                            String mypred = newMap.get(exactPort).split(",")[0];
                            String mysucc = newMap.get(exactPort).split(",")[1];;
                            selfPortValue.setNextPort(mysucc);
                            selfPortValue.setPrevPort(mypred);
                            ObjectOutputStream oos= new ObjectOutputStream(socket.getOutputStream());
                            String pred = newMap.get(reqPort).split(",")[0];
                            String succ =  newMap.get(reqPort).split(",")[1];
                            Node n = new Node();
                            n.setExactPort(serverNode.getExactPort());
                            n.setType("changeValues");
                            n.setNextPort(succ);
                            n.setPrevPort(pred);
                            oos.writeObject(n);
                        }else if(serverNode.getType().equals("changePrev")){
                            selfPortValue.setPrevPort(serverNode.getExactPort());

                        }else if(serverNode.getType().equals("changeNext")){
                            selfPortValue.setNextPort(serverNode.getExactPort());

                        }else if(serverNode.getType().equals("sendToNext")){

                            ObjectOutputStream oos= new ObjectOutputStream(socket.getOutputStream());

                            ContentValues mContentValues = new ContentValues();
                            mContentValues.put("key", serverNode.getMsgKey());
                            mContentValues.put("value", serverNode.getMsgValue());
                          Uri newUri = getContext().getContentResolver().insert(mUri, mContentValues);
                            if(newUri!=null){
                                serverNode.setType("pushed");
                             oos.writeObject(serverNode);

                            }
                            else{
                                serverNode.setType("searching");
                                oos.writeObject(serverNode);

                            }
                        }else if(serverNode.getType().equals("requestedPortValues")){
                            Log.e("server",serverNode.getExactPort()+"..ports");
                            ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());
                            ArrayList<String> a=new ArrayList<String>();
                            for(Map.Entry<String,String> mp:hmPort.entrySet()){
                                a.add(mp.getValue());
                            }
                            serverNode.setForSpecific(a);
                            dos.writeObject(serverNode);

                        }
                        else if (serverNode.getType().equals("retreiving")) {
                            String[] projection = {"key", "value"};
                            Cursor results = query(mUri, projection, serverNode.getMsgKey(), null, "");
                            ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());
                            if (results == null) {
                                serverNode.setType("searching");
                            } else {
                                if (results.getCount() >= 1) {
                                    while (results.moveToNext()) {
                                        serverNode.setType("found");
                                        serverNode.setMsgValue(results.getString(1));
                                        dos.writeObject(serverNode);
                                    }


                                }

                            }
                        } else if(serverNode.getType().equals("each")) {
                            String[] projection = {"key", "value"};
                            //self querying for all keys at its own directory
                            Cursor results = query(mUri, projection, "@", null, "");
                            HashMap<String,String> keyValue= new HashMap<String, String>();
                            if (results.getCount() >= 1) {
                                while (results.moveToNext()) {
                                    keyValue.put(results.getString(0),results.getString(1));
                                }
                            }
                            serverNode.setForAll(keyValue);
                            ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());
                            dos.writeObject(serverNode);
                        }

                }
            }catch(Exception e)
            {
                e.printStackTrace();
            }

        }
    }


    public class ClientThread extends Thread {
        Node node;

        public ClientThread(Node node) {
            this.node = node;
        }

        public void run() {
            try {
                if (node.getType().equals("requestForJoin")) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt("11108"));
                    ObjectOutputStream obj = new ObjectOutputStream(socket.getOutputStream());
                    obj.writeObject(node);
                    ObjectInputStream ois= new ObjectInputStream(socket.getInputStream());
                    Node clientNode=(Node)ois.readObject();

                    if(clientNode.getType().equals("changeValues")){
                        selfPortValue.setPrevPort(clientNode.getPrevPort());
                        selfPortValue.setNextPort(clientNode.getNextPort());
                        if(!clientNode.getNextPort().equals("5554")){
                            Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 2 * Integer.parseInt(clientNode.getNextPort()));
                            ObjectOutputStream obj1 = new ObjectOutputStream(socket1.getOutputStream());
                            Node n = new Node();
                            n.setType("changePrev");
                            n.setExactPort(clientNode.getExactPort());
                            n.setPrevPort(clientNode.getPrevPort());
                            n.setNextPort(clientNode.getNextPort());
                            obj1.writeObject(n);
                        }
                        if(!clientNode.getPrevPort().equals("5554")){
                            Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 2 * Integer.parseInt(clientNode.getPrevPort()));
                            ObjectOutputStream obj1 = new ObjectOutputStream(socket1.getOutputStream());
                            Node n = new Node();
                            n.setType("changeNext");
                            n.setExactPort(clientNode.getExactPort());
                            n.setPrevPort(clientNode.getPrevPort());
                            n.setNextPort(clientNode.getNextPort());
                            obj1.writeObject(n);
                        }
                    }

                }

                else if(node.getType().equals("sendToNext")){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 2 * Integer.parseInt(node.getNextPort()));
                    ObjectOutputStream obj1 = new ObjectOutputStream(socket.getOutputStream());
                    obj1.writeObject(node);
                    ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
                    Node n= (Node) i.readObject();
                    if(n.getType().equals("pushed")){
                        socket.close();
                    }else{
                        socket.close();
                    }
                }

            }catch(Exception e)
            {
                e.getMessage();
            }

        }

        public String[] selectValueForKey() {
            try {

                if (node.getType().equals("retreiving")) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 2 * Integer.parseInt(selfPortValue.getNextPort()));
                    ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
                    o.writeObject(node);
                    ObjectInputStream i= new ObjectInputStream(socket.getInputStream());
                    Node n = (Node) i.readObject();
                    if (n.getType().equals("searching")) {
                        socket.close();
                    } else {

                        socket.close();
                        String[] trial = {n.getMsgKey(),n.getMsgValue()};
                        return trial;


                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        public ArrayList<String> getAllFromEachPort() {
            ArrayList<String> as = new ArrayList<String>();

            try {
                if(node.getType().equals("each")) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 2*Integer.parseInt(node.getExactPort()));
                    ObjectOutputStream oos1 = new ObjectOutputStream(socket.getOutputStream());
                    oos1.writeObject(node);
                    ObjectInputStream ois1 = new ObjectInputStream(socket.getInputStream());
                   Node keyValue= (Node) ois1.readObject();
                    for(Map.Entry<String,String> mp:keyValue.getForAll().entrySet()){
                        as.add(mp.getKey()+","+mp.getValue());
                    }

                    socket.close();



                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return as;
        }


        public ArrayList<String> portsRetrieval() {
            ArrayList<String> a = new ArrayList<String>();

            try {
                if(node.getType().equals("requestedPortValues")) {
                        Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt("11108"));
                        ObjectOutputStream oos1 = new ObjectOutputStream(socket1.getOutputStream());
                        oos1.writeObject(node);
                        ObjectInputStream ois1 = new ObjectInputStream(socket1.getInputStream());
                        Node n = (Node)ois1.readObject();
                        a=n.getForSpecific();
                        return  a;
                }
            } catch (Exception e) {
            }

            return a;
        }
    }
    }

