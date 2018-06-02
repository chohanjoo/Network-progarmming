import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

public class DictionaryImpl extends UnicastRemoteObject implements Dictionary {

	private static final long serialVersionUID = 1L;

	Vector<String> dictionary;

	public DictionaryImpl() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
		dictionary = new Vector<>();
		setDictionary();
	}

	@Override
	public boolean checkWord(String word) throws RemoteException {
		// TODO Auto-generated method stub

		for (int i = 0; i < dictionary.size(); ++i)
			if (dictionary.get(i).compareTo(word) == 0)
				return true;

		return false;
	}

	@Override
	public void setDictionary() throws RemoteException {
		// TODO Auto-generated method stub
		try {
			File file = new File("C:\\Users\\ÇÑÁÖ\\eclipse-workspace\\netword_project\\dictionary.txt");
			FileReader filereader = new FileReader(file);

			BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			String line = "";

			while ((line = bufReader.readLine()) != null)
				dictionary.add(line);

			bufReader.close();
		} catch (FileNotFoundException fe) {
			System.out.println(fe);
		} catch (IOException ie) {
			System.out.println(ie);
		}

	}

}
