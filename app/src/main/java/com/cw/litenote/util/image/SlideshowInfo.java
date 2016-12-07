package com.cw.litenote.util.image;

import java.util.ArrayList;
import java.util.List;

public class SlideshowInfo
{
	private List<String> imageList; // this slideshow's images
   
   // constructor 
   public SlideshowInfo()
   {
      imageList = new ArrayList<String>(); 
   }

   // return List of Strings pointing to the slideshow's images
   public List<String> getImageList()
   {
      return imageList;
   }

   // add a new image path
   public void addImage(String path)
   {
	  System.out.println("path = " + path); 
      imageList.add(path);
   }
   
   // return String at position index
   public String getImageAt(int index)
   {
      if (index >= 0 && index < imageList.size())
         return imageList.get(index);
      else
         return null;
   }
   
   // return number of images/videos in the slideshow
   public int imageSize()
   {
      return imageList.size();
   }
}