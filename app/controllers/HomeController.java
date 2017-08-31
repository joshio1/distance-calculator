package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
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

    /**
     * This method will fetch the POST request, store the location in the database, and return the total distance travelled by the user.
     * @return
     */
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
        double totalDistance = process(latitude, longitude, username, timestamp, start);
        Logger.info("Total Distance: " + totalDistance);
        result.put("total_distance", totalDistance);
        return ok(result);
    }

    /**
     * Stores the current latitude, longitude, username, timestamp, and start flag. Calculates total distances, updates, and stores it.
     *
     * If start flag is true, user has just started his travel. Total distance travelled will be zero.
     * If start flag is false, these are subsequent values, hence total distance since start will be calculated.
     *
     * Total distance will be stored for every POST request. For every new request, total distance will be the previous
     * total distance plus the current displacement.
     * @param currentLat
     * @param currentLon
     * @param username
     * @param timestamp
     * @param start
     */
    public double process(double currentLat, double currentLon, String username, String timestamp, String start){
        try (Connection connection = db.getConnection();){
            String querySql = "SELECT id, username, latitude, longitude, total_distance " +
                    "FROM distance_calculator where username = ? ORDER BY timestamp DESC LIMIT 1";
            PreparedStatement pstmt = connection.prepareStatement(querySql);
            pstmt.setString(1, username);
            ResultSet rs  = pstmt.executeQuery();
            if(rs.next()){
                double previousLat = rs.getDouble("latitude");
                double previousLon = rs.getDouble("longitude");
                double currentDistance = rs.getDouble("total_distance");
                int id = rs.getInt("id");
                double disp = getDisplacement(previousLat, previousLon, currentLat, currentLon);
                double totalDistance = disp + currentDistance;
                Logger.info("Total Distance: "+totalDistance);

                //Inserting location into database.
                //Fetch total distance since "start" for the user.
                if (start!=null && start.equalsIgnoreCase("true")) {
                    //If new measurement, i.e. start is true, then insert into the database
                    String insertSql = "INSERT INTO distance_calculator(username, latitude, longitude, timestamp, total_distance) VALUES(?,?,?,?,?)";
                    PreparedStatement pstmt1 = connection.prepareStatement(insertSql);
                    pstmt1.setString(1, username);
                    pstmt1.setDouble(2, currentLat);
                    pstmt1.setDouble(3, currentLon);
                    pstmt1.setString(4, timestamp);
                    //If start is true, total distance will be zero.
                    pstmt1.setDouble(5, 0);
                    pstmt1.executeUpdate();
                }
                else {
                    // Else total distance will be displacement between current co-ordinates and previous co-ordinates
                    // for the user from database plus the total distance upto the previous co-ordinates.
                    Logger.info("Calculating total distance since start for: " + username);

                    String insertSql = "UPDATE distance_calculator SET latitude = ? , longitude = ?, timestamp = ?, total_distance = ? WHERE id = ?";
                    PreparedStatement pstmt2 = connection.prepareStatement(insertSql);
                    pstmt2.setDouble(1, currentLat);
                    pstmt2.setDouble(2, currentLon);
                    pstmt2.setString(3, timestamp);
                    pstmt2.setDouble(4, totalDistance);
                    pstmt2.setInt(5, id);
                    pstmt2.executeUpdate();
                }
                return totalDistance;
            }
        }catch (Exception e){
            Logger.error("Error while processing", e);
        }
        return 0.0;
    }

    /**
     * Gets the displacement between two GPS co-ordinates.
     * @param previousLat
     * @param previousLon
     * @param currentLat
     * @param currentLon
     * @return
     */
    public double getDisplacement(double previousLat, double previousLon, double currentLat, double currentLon){
        double theta = previousLon - currentLon;
        double dist = Math.sin(deg2rad(previousLat)) * Math.sin(deg2rad(currentLat)) + Math.cos(deg2rad(previousLat)) * Math.cos(deg2rad(currentLat)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        Logger.info("Displacement: "+dist);
        return dist;
        // Reference - https://dzone.com/articles/distance-calculation-using-3
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}