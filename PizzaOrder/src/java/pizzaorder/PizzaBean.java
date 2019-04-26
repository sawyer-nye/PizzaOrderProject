package pizzaorder;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

// define the data source
@DataSourceDefinition(
    name = "java:global/jdbc/pizzadb",
    className = "org.apache.derby.jdbc.ClientDataSource",
    url = "jdbc:derby://localhost:1527/pizzadb",
    databaseName = "pizzadb",
    user = "app",
    password = "app"
)

@ManagedBean(name="pizzaBean")
@SessionScoped
public class PizzaBean implements Serializable {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    
    // initialize all IDs to first selection, quantities to 0
    private String pizzaId = "1";
    private String pizzaQn = "0";
    private String sidesId = "1";
    private String sidesQn = "0";
    private String drinkId = "1";
    private String drinkQn = "0";
    
    private String orderId;
    
    
    // allow server to inject the DataSource
    @Resource(lookup="java:global/jdbc/pizzadb")
    DataSource dataSource;
    
    // Below: getters and setters
    // Note:  all have type String returns for easy database insertion
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPizzaId() {
        return pizzaId;
    }
    
    public void setPizzaId(String pizzaId) {
        this.pizzaId = pizzaId;
    }
    
    public String getPizzaQn() {
        return pizzaQn;
    }
    
    public void setPizzaQn(String pizzaQn) {
        this.pizzaQn = pizzaQn;
    }
    
    public String getSidesId() {
        return sidesId;
    }
    
    public void setSidesId(String sidesId) {
        this.sidesId = sidesId;
    }
    
    public String getSidesQn() {
        return sidesQn;
    }
    
    public void setSidesQn(String sidesQn) {
        this.sidesQn = sidesQn;
    }
    
    public String getDrinkId() {
        return drinkId;
    }
    
    public void setDrinkId(String drinkId) {
        this.drinkId = drinkId;
    }
    
    public String getDrinkQn() {
        return drinkQn;
    }
    
    public void setDrinkQn(String drinkQn) {
        this.drinkQn = drinkQn;
    }
    
    // calculates price of customer's pizza order
    public int getPizzaPrice() {
        int pizzaPrice = 0;
        
        switch (getPizzaId()) {
            // pizzaPrice = price/category * quantity
            case "1":           // pepperoni
                pizzaPrice = (Integer.parseInt(getPizzaQn()) * 8);
                break;
            case "2":           // sausage
                pizzaPrice = (Integer.parseInt(getPizzaQn()) * 9);
                break;
            case "3":           // cheese
                pizzaPrice = (Integer.parseInt(getPizzaQn()) * 6);
                break;
        }
        return pizzaPrice;
    }
    
    // calculates price of customer's sides order
    public int getSidesPrice() {
        int sidesPrice = 0;
        
        switch (getSidesId()) {
            // sidesPrice = price/category * quantity
            case "1":           // cheese sticks
                sidesPrice = (Integer.parseInt(getSidesQn()) * 4);
                break;
            case "2":           // buffalo wings
                sidesPrice = (Integer.parseInt(getSidesQn()) * 6);
                break;
        }
        return sidesPrice;
    }
    
    // calculates price of customer's drink order
    public int getDrinkPrice() {
        int drinkPrice = 0;
        
        switch (getDrinkId()) {
            // drinkPrice = price/category * quantity
            case "1":           // pepsi
                drinkPrice = (Integer.parseInt(getDrinkQn()) * 2);
                break;
            case "2":           // water
                drinkPrice = (Integer.parseInt(getDrinkQn()) * 1);
                break;
        }
        return drinkPrice;
    }
    
    // sums calculated pizza, sides, and drink prices
    public String getTotalPrice() {
        return Integer.toString(getPizzaPrice() + getSidesPrice() + getDrinkPrice());
    }
    
    // creates resultset table for orders sorted by descending
    public ResultSet getOrders() throws SQLException {
        // check if dataSource was injected by the server
        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource");
        }

        // obtain connection from connection pool
        Connection connection = dataSource.getConnection();

        // check whether connection was successful
        if (connection == null) {
            throw new SQLException("Unable to connect to DataSource");
        }

        try {
            // create a PreparedStatement to select orders by ordertime descending
            PreparedStatement getOrders = connection.prepareStatement(
            "SELECT * FROM APP.ORDERS ORDER BY ORDERTIME DESC");
            
            // populates table with retrieved data
            CachedRowSet rowSet =
                RowSetProvider.newFactory().createCachedRowSet();
            rowSet.populate(getOrders.executeQuery());
            return rowSet;
        }
        finally {
            // return this connection to pool
            connection.close();
        }
    }
    
    // save a new pizza order to database
    public String orderPizza() throws SQLException {
        // check if dataSource was injected by server
        if (dataSource == null) {
            throw new SQLException("Unable to obtain DataSource");
        }
        
        // obtain connection from connection pool
        Connection connection = dataSource.getConnection();
        
        // check if connection was successful
        if (connection == null) {
            throw new SQLException("Unable to connect to DataSource");
        }
        
        try {
            /*  Generates an orderId based on current date
                Note: this doesn't receive its own method so that no conflicting
                      orderId's are created when each query is created below. */
            Date orderDate = new Date();
            orderId = Integer.toString(
                (int) (orderDate.getTime() & 0x0000000000ffffffL));
            
            // generates orderTime which goes into ORDERS table
            String orderTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(orderDate);
            
            // Creates query for new entry to CUSTOMER Table only if not in table
            PreparedStatement addCustomer =
                connection.prepareStatement(
                    "INSERT INTO CUSTOMERS (PHONENUMBER, FIRSTNAME, LASTNAME, EMAIL) " +
                    "(SELECT ?, ?, ?, ? FROM CUSTOMERS WHERE PHONENUMBER = ? " +
                    "HAVING COUNT(*) = 0)");
            
            addCustomer.setString(1, getPhone());      // sets 1st ? to phone
            addCustomer.setString(2, getFirstName());  // sets 2nd ? to firstName
            addCustomer.setString(3, getLastName());   // sets 3rd ? to lastName
            addCustomer.setString(4, getEmail());      // sets 4th ? to email
            addCustomer.setString(5, getPhone());      // sets 5th ? to email   
            
            // Creates query for new entry to ORDEREDITEMS table
            PreparedStatement addOrderedItems =
                connection.prepareStatement(
                    "INSERT INTO ORDEREDITEMS" +
                    "(ORDERID, PIZZAID, PIZZAQN, SIDESID, SIDESQN, DRINKID, DRINKQN)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)");
            
            addOrderedItems.setString(1, orderId);      // sets 1st ? to orderId
            addOrderedItems.setString(2, getPizzaId()); // sets 2nd ? to pizzaId
            addOrderedItems.setString(3, getPizzaQn()); // sets 3rd ? to pizzaQn
            addOrderedItems.setString(4, getSidesId()); // etc.
            addOrderedItems.setString(5, getSidesQn());
            addOrderedItems.setString(6, getDrinkId());
            addOrderedItems.setString(7, getDrinkQn());
            
            // Creates query for new entry to ORDERS table
            PreparedStatement addOrder =
                connection.prepareStatement(
                    "INSERT INTO ORDERS" +
                    "(ORDERID, PHONENUMBER, ORDERTIME, TOTALPRICE)" +
                    "VALUES (?, ?, ?, ?)");
            
            addOrder.setString(1, orderId);
            addOrder.setString(2, getPhone());
            addOrder.setString(3, orderTime);
            addOrder.setString(4, getTotalPrice());
            
            // executes queries
            addCustomer.executeUpdate();        // insert the new customer entry
            addOrderedItems.executeUpdate();    // insert the new ordered items entry
            addOrder.executeUpdate();           // insert the new order entry
            
            return "index?faces-redirect=true"; // go back to index.xhtml page
        }
        finally {
            connection.close();                 // return this connection to pool
        }
    }
    
}
