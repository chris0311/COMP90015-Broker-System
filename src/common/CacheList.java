/**
 * @Author: Chris Liang 1159696
 */

package common;

import java.util.LinkedList;

public class CacheList {
    private LinkedList<String> cacheList;
    private int maxSize;

    public CacheList(int maxSize) {
        this.cacheList = new LinkedList<>();
        this.maxSize = maxSize;
    }

    public void add(String item) {
        if (cacheList.size() == maxSize) {
            cacheList.removeFirst();
        }
        cacheList.add(item);
    }

    public boolean contains(String item) {
        return cacheList.contains(item);
    }

    public void remove(String item) {
        cacheList.remove(item);
    }

    public void clear() {
        cacheList.clear();
    }

    public int size() {
        return cacheList.size();
    }

    public String get(int index) {
        return cacheList.get(index);
    }

    public LinkedList<String> getCacheList() {
        return cacheList;
    }

}
