package fr.rhaz.os.android.rss;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haz on 04/08/2016.
 */
public class Handler extends DefaultHandler {
    private List<PluginRSS> plugins;
    private PluginRSS plugin;
    private boolean title;
    private boolean link;
    private boolean description;

    public Handler(){
        plugins = new ArrayList<>();
    }

    public List<PluginRSS> getPlugins(){
        return plugins;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch(qName){
            case "item":
                plugin = new PluginRSS();
                break;
            case "title":
                title = true;
                break;
            case "description":
                description = true;
                break;
            case "link":
                link = true;
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch(qName){
            case "item":
                plugins.add(plugin);
                plugin = null;
                break;
            case "title":
                title = false;
                break;
            case "description":
                description = false;
                break;
            case "link":
                link = false;
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(plugin != null){
            if(title) plugin.setTitle(new String(ch, start, length));
            if(link) plugin.setLink(new String(ch, start, length));
            if(description) plugin.setDescription(new String(ch, start, length));
        }
    }
}
