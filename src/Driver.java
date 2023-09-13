import org.openstreetmap.gui.jmapviewer.*;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
 
import javax.imageio.ImageIO;
import javax.swing.*;
 
public class Driver {
               
                public static int animationSec;
                public static ArrayList<TripPoint> trip;
                public static BufferedImage arrow;
 
    public static void main(String[] args) throws FileNotFoundException, IOException {
                TripPoint.readFile("triplog.csv");
                TripPoint.h2StopDetection();
                arrow = ImageIO.read(new File("arrow.png"));
               
                // set up frame
        JFrame frame = new JFrame("Map Viewer");
        frame.setLayout(new BorderLayout());
       
        // set up top panel for input selections
        JPanel topPanel = new JPanel();
        frame.add(topPanel, BorderLayout.NORTH);
        // play button
        JButton play = new JButton("Play");
        // checkbox to enable/disable stops
        JCheckBox includeStops = new JCheckBox("Include Stops");
        // dropbox to pick animation time
        String[] timeList = {"Animation Time", "15", "30", "60", "90"};
        JComboBox<String> animationTime = new JComboBox<String>(timeList);
        animationSec = 0;
        // add all to top panel
        topPanel.add(animationTime);
        topPanel.add(includeStops);
        topPanel.add(play);
       
        // set up mapViewer
        JMapViewer mapViewer = new JMapViewer();
        frame.add(mapViewer);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mapViewer.setTileSource(new OsmTileSource.TransportMap());
       
        // add listeners
        play.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                              mapViewer.removeAllMapMarkers(); // remove all markers from the map
                              mapViewer.removeAllMapPolygons();
                              if (includeStops.isSelected()) {
                                              trip = TripPoint.getTrip();
                              }
                              else {
                                              trip = TripPoint.getMovingTrip();
                              }
                        plotTrip(animationSec, trip, mapViewer);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        animationTime.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object selectedItem = animationTime.getSelectedItem();
                    if (selectedItem instanceof String) {
                        String selectedString = (String) selectedItem;
                        if (!selectedString.equals("Animation Time")) {
                            animationSec = Integer.parseInt(selectedString);
                            System.out.println("Updated to " + animationSec);
                        }
                    }
                }
            }
       });
 
        // set the map center and zoom level
        mapViewer.setDisplayPosition(new Coordinate(34.50, -107.99), 6);
       
    }
   
    
 
   
    public static void plotTrip(int seconds, ArrayList<TripPoint> trip, JMapViewer map) {
        // amount of time between each point in milliseconds
        int delayTime = seconds * 1000 / trip.size();
 
 
        Timer timer = new Timer(delayTime, new ActionListener() {
                Coordinate c1;
             Coordinate c2 = null;
             MapMarker marker;
             MapMarker prevMarker = null;
             MapPolygonImpl line;
             int currentIndex = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the current point and create the marker
                c1 = new Coordinate(trip.get(currentIndex).getLat(), trip.get(currentIndex).getLon());
                if (currentIndex != 0) {
                    c2 = new Coordinate(trip.get(currentIndex - 1).getLat(), trip.get(currentIndex - 1).getLon());
                    double angle = Math.toDegrees(Math.atan2(c2.getLon() - c1.getLon(), c2.getLat() - c1.getLat())) + 180;
                    BufferedImage rotatedArrow = rotateImage(arrow, angle);
                    marker = new IconMarker(c1, rotatedArrow);
                    line = new MapPolygonImpl(c1, c2, c2);
                    line.setColor(Color.RED);
                    line.setStroke(new BasicStroke(3));
                    map.addMapPolygon(line);
                    map.removeMapMarker(prevMarker);
                } else {
                    // For the first point, use a non-rotated arrow
                    marker = new IconMarker(c1, arrow);
                }
 
                // Add the marker to the map and update the previous marker
                map.addMapMarker(marker);
                prevMarker = marker;
 
                // Increment the current index, and stop the timer if we've reached the end of the trip
                currentIndex++;
                if (currentIndex >= trip.size()) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();
    }
 
 
    public static BufferedImage rotateImage(BufferedImage image, double angleInDegrees) {
        int w = image.getWidth();
        int h = image.getHeight();
 
        int newW = (int) (w * Math.abs(Math.cos(Math.toRadians(angleInDegrees))) +
                          h * Math.abs(Math.sin(Math.toRadians(angleInDegrees))));
        int newH = (int) (h * Math.abs(Math.cos(Math.toRadians(angleInDegrees))) +
                          w * Math.abs(Math.sin(Math.toRadians(angleInDegrees))));
 
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate(newW / 2, newH / 2); // Set rotation center at the center of the new image
        at.rotate(Math.toRadians(angleInDegrees));
        at.translate(-w / 2, -h / 2); // Translate back to the top-left corner of the original image
        g2d.setTransform(at);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return rotated;
    }
 
 
}
 