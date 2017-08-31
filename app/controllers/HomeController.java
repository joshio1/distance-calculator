package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import org.springframework.beans.factory.parsing.Location;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.Database;
import play.libs.Json;
import play.mvc.*;

import java.sql.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject
    FormFactory formFactory;

    @Inject
    Database db;
    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

    public Result handleupdates() {
        DynamicForm dynamicForm = formFactory.form().bindFromRequest();
        Logger.info("Username is: " + dynamicForm.get("username"));
        Logger.info("Time is: " + dynamicForm.get("timestamp"));
        Logger.info("Latitude is: " + dynamicForm.get("latitude"));
        Logger.info("Longitude is: " + dynamicForm.get("longitude"));
        Logger.info("Start is: " + dynamicForm.get("start"));
        String username = dynamicForm.get("username");
        String timestamp = dynamicForm.get("timestamp");
        double latitude = Double.parseDouble(dynamicForm.get("latitude"));
        double longitude = Double.parseDouble(dynamicForm.get("longitude"));
        String start = dynamicForm.get("start");
        ObjectNode result = Json.newObject();
//        result.put("total_distance", 0.0);
        insertIntoDatabase(latitude, longitude, username, timestamp, start);
        Logger.info("Total Distance after insert: " + totalDistance);
        result.put("total_distance", totalDistance);
//        return ok("ok, I received POST data. That's all…\n : "+totalDistance);
        return ok(result);
    }

    public void insertIntoDatabase(double latitude, double longitude, String username, String timestamp, String start){
        db.withConnection(connection -> {
            // do whatever you need with the db connection
            String createSql = "CREATE TABLE IF NOT EXISTS distance_calculator (\n"
                    + "	id integer PRIMARY KEY,\n"
                    + "	username text NOT NULL,\n"
                    + "	latitude real NOT NULL,\n"
                    + "	longitude real NOT NULL,\n"
                    + "	timestamp text NOT NULL,\n"
                    + "	total_distance real NOT NULL\n"
                    + ");";
            Logger.info(createSql);
            String insertSql = "INSERT INTO distance_calculator(username, latitude, longitude, timestamp, total_distance) VALUES(?,?,?,?,?)";
            Statement stmt = connection.createStatement();
            // create a new table
            stmt.execute(createSql);

            PreparedStatement pstmt = connection.prepareStatement(insertSql);
            pstmt.setString(1, username);
            pstmt.setDouble(2, latitude);
            pstmt.setDouble(3, longitude);
            pstmt.setString(4, timestamp);
            totalDistance = 0.0;
            if (start!=null && start.equalsIgnoreCase("true"))
                pstmt.setDouble(5, 0);
            else{
                Logger.info("Start not found!");
                getTotalDistance(username, latitude, longitude);
                pstmt.setDouble(5, totalDistance);
                Logger.info("Total Distance Before Insert: "+totalDistance);
            }
            pstmt.executeUpdate();
        });
    }

    double totalDistance = 0.0;
    public void getTotalDistance(String username, double currentLat, double currentLon){

        db.withConnection(connection -> {

            String querySql = "SELECT username, latitude, longitude, total_distance " +
                    "FROM distance_calculator where username = ? ORDER BY timestamp DESC LIMIT 1";
            PreparedStatement pstmt = connection.prepareStatement(querySql);
            pstmt.setString(1, username);
            ResultSet rs  = pstmt.executeQuery();
            totalDistance = 0.0;
            if(rs.next()){
                double previousLat = rs.getDouble("latitude");
                double previousLon = rs.getDouble("longitude");
                double currentDistance = rs.getDouble("total_distance");
                double disp = getDisplacement(previousLat, previousLon, currentLat, currentLon);
                totalDistance = disp + currentDistance;
                Logger.info("Total Distance: "+totalDistance);
            }
        });
    }

    public double getDisplacement(double previousLat, double previousLon, double currentLat, double currentLon){
        double theta = previousLon - currentLon;
        double dist = Math.sin(deg2rad(previousLat)) * Math.sin(deg2rad(currentLat)) + Math.cos(deg2rad(previousLat)) * Math.cos(deg2rad(currentLat)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        Logger.info("Displacement: "+dist);
        return dist;
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}