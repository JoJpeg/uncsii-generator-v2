package core;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

public class Project {
    private Map<String, ImageModel> imgs;

    public Project() {
        this.imgs = new HashMap<>();
    }

    public Map<String, ImageModel> getImgs() {
        return imgs;
    }

    public void setImgs(Map<String, ImageModel> imgs) {
        this.imgs = imgs;
    }
    
    public void addImage(String name, ImageModel model) {
        this.imgs.put(name, model);
        model.setName(name);
    }

    public ImageModel getImage(String name){
        for (ImageModel img : imgs.values()) {
            if(img.getName().equals(name)){
                return img;
            }
        }
        return null;
    }
}
