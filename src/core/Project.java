package core;

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
    }
}
