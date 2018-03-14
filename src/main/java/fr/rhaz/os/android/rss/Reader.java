package fr.rhaz.os.android.rss;

import android.os.AsyncTask;

import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Reader extends AsyncTask<String, Void, List<PluginRSS>>{
    private List<PluginRSS> plugins;
    private Exception exception;

    public Reader(){
        super();
        plugins = new ArrayList<>();
    }

    @Override
    protected List<PluginRSS> doInBackground(String... url) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = parser = factory.newSAXParser();

            Handler handler = new Handler();
            parser.parse(url[0], handler);

            plugins = handler.getPlugins();
        } catch(Exception e){
            exception = e;
        } return plugins;
    }

    public void getException() throws Exception{
        if(exception != null) throw exception;
    }

    public List<PluginRSS> getPlugins(){
        return plugins;
    }
}
