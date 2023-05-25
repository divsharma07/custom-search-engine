package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import model.Document;

public class Helper {


  public static void saveDocumentList(List<Document> o, String filename) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(filename);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(o);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  public static List<Document> loadDocumentList(String filename) {
    FileInputStream in;
    ObjectInputStream ois;

    List<Document> o = null;
    String path = new File("").getAbsolutePath() + "\\" + filename;

    try {
      in = new FileInputStream(path);
      ois = new ObjectInputStream(in);
      o = (List<Document>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return o;
  }

  public static void saveOkapiScore(Map<String, Double> o, String filename) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(filename);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(o);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, Double> loadOkapiScore(String filename) {
    FileInputStream in;
    ObjectInputStream ois;

    Map<String, Double> o = null;
    String path = new File("").getAbsolutePath() + "\\" + filename;

    try {
      in = new FileInputStream(path);
      ois = new ObjectInputStream(in);
      o = (Map<String, Double>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return o;
  }

  public static boolean fileExists(String fileName) {
    String absPath = new File("").getAbsolutePath();
    File f = new File(absPath + "\\" + fileName);
    return f.exists() && !f.isDirectory();
  }
}
