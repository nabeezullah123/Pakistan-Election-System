

import java.io.*; 
import java.net.*; 
import java.sql.*; 

public class VoterServer {
    private static final int PORT = 12345; 
 
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { 
            System.out.println("Server started and listening on port " + PORT); 
            while (true) { 
                try (Socket socket = serverSocket.accept()) { 
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true); 
                    String request = in.readLine(); 
                    String[] parts = request.split(";", 2); 
                    String command = parts[0]; 
                    String data = parts.length > 1 ? parts[1] : ""; 
                    String response = handleCommand(command, data); 
                    out.println(response); 
                } catch (Exception e) { 
                    e.printStackTrace(); 
                } 
            } 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
 
    private static String handleCommand(String command, String data) { 
    switch (command.toLowerCase()) { 
        case "@": return insertVoter(data); 
        case "@@":
            return updateVoter(data); 
        case "@@@":
            return deleteVoter(data); 
        case "@@@@":
            return getAllUsers(data); 
        case "$":
            return insertCandidate(data);
        case "#":
            return insertparty(data);
        case "&":
            return insertvote(data);
            
             case "&&":
            return getAllPrties(data);

        case "signup":
            return signupUser(data);  // ➕ added
        case "login":
            return loginUser(data);    // ➕ added
case "$$": // new command to get all candidates
    return getAllCandidates();
case "$$$":
    return updateCandidate(data);

        default: return "Invalid Command"; 
    } 
}

    private static Connection getConnection() throws SQLException { 
        String url = "jdbc:mysql://localhost:3306/election_sys"; 
        String user = "root"; // replace 
        String password = "nabeez12@"; // replace 
        return DriverManager.getConnection(url, user, password); 
    } 
 
 
    private static String insertVoter(String data) { 
        String[] fields = data.split(","); 
        if (fields.length != 5) return "Invalid Data"; 
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement("insert into voter(cnic,name,constituency_id,city,province) values(?,?,?,?,?);")) { 
            stmt.setString(1, fields[0]); 
            stmt.setString(2, fields[1]); 
            stmt.setString(3, fields[2]); 
            stmt.setString(4, fields[3]); 
            stmt.setString(5, fields[4]); 
            stmt.executeUpdate(); 
            return "Voter Inserted Successfully"; 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 
    
    
 
    private static String updateVoter(String data) { 
        String[] fields = data.split(","); 
        if (fields.length != 5) return "Invalid Data"; 
        try (Connection conn = getConnection(); 
             CallableStatement stmt = conn.prepareCall("{CALL UpdateVoter(?,?,?,?,?)}")) { 
            stmt.setString(1, fields[0]); 
            stmt.setString(2, fields[1]); 
            stmt.setString(3, fields[2]); 
            stmt.setString(4, fields[3]); 
            stmt.setString(5, fields[4]); 
            stmt.executeUpdate(); 
            return "Voter Updated Successfully"; 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 
    
     
 
    private static String deleteVoter(String data) { 
        String cnic = data.trim(); 
 
        try (Connection conn = getConnection(); 
             CallableStatement stmt = conn.prepareCall("{CALL DeleteVoter(?)}")) { 
            stmt.setString(1, cnic); 
            stmt.executeUpdate(); 
            return "Voter Deleted Successfully"; 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 
 
    private static String getAllUsers(String data) { 
        StringBuilder sb = new StringBuilder(); 
        try (Connection conn = getConnection(); 
             CallableStatement stmt = conn.prepareCall("select * from users"); 
             ResultSet rs = stmt.executeQuery()) { 
            while (rs.next()) { 
                sb.append(rs.getString("cnic")).append(",") 
                  .append(rs.getString("name")).append(",") 
                  .append(rs.getString("City")).append(",") 
                  .append(rs.getString("constituency_id")).append(",")
                  .append(rs.getString("province")).append(",")
                        .append(rs.getString("password")).append(",");
                
            } 
            return sb.toString(); 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 

    
    
    
   private static String getAllPrties(String data) { 
    StringBuilder sb = new StringBuilder(); 
    try (Connection conn = getConnection(); 
         CallableStatement stmt = conn.prepareCall("select * from party "); 
         ResultSet rs = stmt.executeQuery()) { 
        
        int count = 0;
        while (rs.next()) { 
            count++;
            System.out.println("Row " + count + ": " + rs.getString("Name")); // Debugging
            sb.append(rs.getString("ID")).append(",") 
              .append(rs.getString("Name")).append(",") 
              .append(rs.getString("Symbol")).append(",") 
              .append(rs.getString("ChairPerson")).append("\n"); // or \n for console
        } 
        
        if (count == 0) {
            return "No data found.";
        }

        return sb.toString(); 
    } catch (SQLException e) { 
        e.printStackTrace(); 
        return "Error: " + e.getMessage(); 
    } 
}


    
        private static String insertCandidate(String data) { 
     
    String[] fields = data.split(",");
    if (fields.length != 4) return "❌ Error: Incomplete Data Provided.";

    String cnic = fields[0].trim();
    String name = fields[1].trim();
    int constituencyNo, partyId;

    try {
        constituencyNo = Integer.parseInt(fields[2].trim());
        partyId = Integer.parseInt(fields[3].trim());
    } catch (NumberFormatException e) {
        return "❌ Error: Constituency No and Party ID must be valid integers.";
    }

    try (Connection conn = getConnection()) {
        // ✅ Check if Party ID exists (optional but recommended)
        PreparedStatement checkParty = conn.prepareStatement("SELECT * FROM party WHERE id = ?");
        checkParty.setInt(1, partyId);
        ResultSet rs = checkParty.executeQuery();
        if (!rs.next()) return "❌ Error: Party ID does not exist.";

        // ✅ Insert Candidate
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO candidate (cnic, name, constituencyNo, partyid) VALUES (?, ?, ?, ?)"
        );
        stmt.setString(1, cnic);
        stmt.setString(2, name);
        stmt.setInt(3, constituencyNo);
        stmt.setInt(4, partyId);
        stmt.executeUpdate();

        return "✅ Candidate Registered Successfully";

    } catch (SQLException e) {
        if (e.getMessage().contains("Duplicate entry")) {
            return "❌ Error: CNIC already exists.";
        }
        e.printStackTrace();
        return "❌ SQL Error: " + e.getMessage();
    }

 
    } 
        private static String insertparty(String data) { 
        String[] fields = data.split(","); 
        if (fields.length != 4) return "Invalid Data"; 
        try (Connection conn = getConnection(); 
             PreparedStatement stmt2 = conn.prepareStatement("insert into party(id,name,symbol,chairperson) values(?,?,?,?);")) { 
            stmt2.setString(1, fields[0]); 
            stmt2.setString(2, fields[1]); 
            stmt2.setString(3, fields[2]); 
            stmt2.setString(4,fields[3]); 
            stmt2.executeUpdate(); 
            return "Party Inserted Successfully"; 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 
 
                private static String insertvote(String data) { 
        String[] fields = data.split(","); 
        if (fields.length != 4) return "Invalid data"; 
        try (Connection conn = getConnection(); 
             PreparedStatement stmt2 = conn.prepareStatement("insert into vote(voterid,CandidateName,SeatNo,PartyName) values(?,?,?,?);")) { 
            stmt2.setString(1, fields[0]); 
            stmt2.setString(2, fields[1]); 
            stmt2.setString(3, fields[2]); 
            stmt2.setString(4, fields[3]); 
            stmt2.executeUpdate(); 
            return "Vote Inserted Successfully"; 
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Error: " + e.getMessage(); 
        } 
    } 
 
private static String signupUser(String data) {
    String[] fields = data.split(",");
    if (fields.length != 6) return "Invalid Data";

    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO users (cnic, name, city, constituency_id, province, password) VALUES (?, ?, ?, ?, ?, ?)")) {

        stmt.setString(1, fields[0]); // cnic
        stmt.setString(2, fields[1]); // name
        stmt.setString(3, fields[2]); // city
        stmt.setString(4, fields[3]); // constituency_id
        stmt.setString(5, fields[4]); // province
        stmt.setString(6, fields[5]); // password

        stmt.executeUpdate();
        return "✅ Registration Successful";

    } catch (SQLException e) {
        if (e.getMessage().contains("Duplicate entry")) {
            return "❌ Error: CNIC already exists.";
        }
        return "❌ Error: " + e.getMessage();
    }
}

private static String loginUser(String data) {
    String[] fields = data.split(",");
    if (fields.length != 2) return "Invalid Data";

    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(
            "SELECT * FROM users WHERE name = ? AND password = ?")) {

        stmt.setString(1, fields[0]); // name (used as username)
        stmt.setString(2, fields[1]); // password

        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return "Login Successful";
        } else {
            return "Error: Invalid name or password";
        }

    } catch (SQLException e) {
        return "Error: " + e.getMessage();
    }
}
private static String getAllCandidates() {
    StringBuilder sb = new StringBuilder();
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM candidate");
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            sb.append(rs.getString("cnic")).append(",")
              .append(rs.getString("name")).append(",")
              .append(rs.getInt("constituencyNo")).append(",")
              .append(rs.getInt("partyid")).append(";");
        }
        return sb.length() == 0 ? "No Candidates Found" : sb.toString();

    } catch (SQLException e) {
        e.printStackTrace();
        return "Error: " + e.getMessage();
    }
}
private static String updateCandidate(String data) {
    String[] fields = data.split(",");
    if (fields.length != 4) return "Error: Incomplete Data";

    String cnic = fields[0].trim();
    String name = fields[1].trim();
    int constituencyNo, partyId;

    try {
        constituencyNo = Integer.parseInt(fields[2].trim());
        partyId = Integer.parseInt(fields[3].trim());
    } catch (NumberFormatException e) {
        return "Error: Constituency No and Party ID must be integers.";
    }

    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "UPDATE candidate SET name = ?, constituencyNo = ?, partyid = ? WHERE cnic = ?")) {

        stmt.setString(1, name);
        stmt.setInt(2, constituencyNo);
        stmt.setInt(3, partyId);
        stmt.setString(4, cnic);

        int updated = stmt.executeUpdate();
        return (updated > 0) ? "✅ Candidate Updated" : "❌ Candidate Not Found";

    } catch (SQLException e) {
        e.printStackTrace();
        return "Error: " + e.getMessage();
    }
}


private static String getResult(String data) {
    String[] fields = data.split(",");
    if (fields.length != 2) return "Invalid Data";

    String cnic = fields[0];
    String year = fields[1];

    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(
                 "SELECT candidate_cnic, total_votes, year, status FROM result WHERE candidate_cnic=? AND year=?")) {

        stmt.setString(1, cnic);
        stmt.setString(2, year);

        ResultSet rs = stmt.executeQuery();
        StringBuilder resultData = new StringBuilder();

        while (rs.next()) {
            resultData.append(rs.getString("CandidateCNIC")).append(",")
                      .append(rs.getInt("VotesTotal")).append(",")
                      .append(rs.getInt("Year")).append(",")
                      .append(rs.getString("Status")).append(";");
        }

        if (resultData.length() == 0) {
            return "No Result Found";
        }

        return resultData.toString();

    } catch (SQLException e) {
        e.printStackTrace();
        return "Error: " + e.getMessage();
    }
}





}
    
    

