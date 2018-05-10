package com.example.utku.shufflemore;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

class AppData {

    static String refreshToken;
    static String accessToken;
    static long expirationTime; //TODO write to file

    static String getRefreshToken(Context context)
    {
        if (refreshToken == null) {
            File file  = new File(context.getFilesDir(), "refresh_token.dat");
            try {
                FileInputStream fileInputStream = new FileInputStream(file);

                StringBuilder builder = new StringBuilder();
                int ch;
                while((ch = fileInputStream.read()) != -1){
                    builder.append((char)ch);
                }

                refreshToken = builder.toString();

            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + file.getPath());
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //System.out.println("Refresh token: " + refreshToken);
        return refreshToken;
    }

    static Vector<String> getHistory(Context context)
    {
        try
        {
            File file  = new File(context.getFilesDir(), "history.dat");
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Vector<String> h = (Vector<String>) in.readObject();
            in.close();
            fileIn.close();
            return h;

        }catch(IOException i) {
            i.printStackTrace();
        }catch(ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }

        return null;
    }

    static void setHistory(Vector<String> history, Context context)
    {
        try {
            File file = new File(context.getFilesDir(), "history.dat");
            FileOutputStream fileOutputStream2 = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream2);
            objectOutputStream.writeObject(history);
            objectOutputStream.close();
            fileOutputStream2.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean tokenExpired()
    {
        return expirationTime <= System.currentTimeMillis();
    }

}

