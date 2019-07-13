package databaseManagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import databaseSerialization.Book;
import databaseSerialization.User;

public class DatabaseConnection {
	
	String url;
	Connection conn = null;
	
	public DatabaseConnection() throws SQLException {
		url = "jdbc:sqlite:BookDatabase.db";
		conn = DriverManager.getConnection(url);
	}
	
	public void close() throws SQLException{
		if(conn != null)
			conn.close();
	}
	
	public ResultSet getCategories() throws SQLException {
		String sql = "SELECT * FROM Category_Types;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		return rs;
	}
	
	public ResultSet getBooksFromCategory(int category) throws SQLException {
		String sql = "SELECT * FROM Books NATURAL JOIN (SELECT ISBN, COUNT(*) AS Num_of_books FROM Books WHERE User_ID IS NULL GROUP BY ISBN) " +
					 "WHERE Category_ID = ? AND User_ID IS NULL GROUP BY ISBN;";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, category);
		ResultSet rs = pstmt.executeQuery();
		return rs;
	}
	
	public ResultSet getUsersNumBooksGroupedByStatus() throws SQLException
	{
		String sql = "SELECT A.User_ID, B.GREEN, C.YELLOW, D.RED, E.BLACK " + 
					 "FROM Books AS A " + 
					 "LEFT JOIN (SELECT User_ID, COUNT(*) AS GREEN FROM Books WHERE Deadline_status = 'GREEN' GROUP BY User_ID) AS B ON A.User_ID = B.User_ID " + 
					 "LEFT JOIN (SELECT User_ID, COUNT(*) AS YELLOW FROM Books WHERE Deadline_status = 'YELLOW' GROUP BY User_ID) AS C ON A.User_ID = C.User_ID " + 
					 "LEFT JOIN (SELECT User_ID, COUNT(*) AS RED FROM Books WHERE Deadline_status = 'RED' GROUP BY User_ID) AS D ON A.User_ID = D.User_ID " + 
					 "LEFT JOIN (SELECT User_ID, COUNT(*) AS BLACK FROM Books WHERE Deadline_status = 'BLACK' GROUP BY User_ID) AS E ON A.User_ID = E.User_ID " + 
					 "WHERE A.User_ID IS NOT NULL "+
					 "GROUP BY A.User_ID;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		return rs;
	}
	
	public ResultSet getUserIDWithEmail() throws SQLException
	{
		String sql = "SELECT Username, E_mail FROM Customer_Account;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		return rs;
	}
	
	public ResultSet getUserDeadlineStatus(String username) throws SQLException
	{
		String sql = "SELECT User_deadline_status FROM Customer_Account WHERE Username = ?;";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, username);
		ResultSet rs = pstmt.executeQuery();
		return rs;
	}
	
	public ResultSet getCustomersList() throws SQLException
	{
		String sql = "SELECT * FROM Customer_Account;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		return rs;
	}
	
	public ResultSet getEmployeesList() throws SQLException
	{
		String sql = "SELECT * FROM Employee_Account;";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		return rs;
	}
	
	public void insertNewBook(Book book, int categoryID, String userID) throws SQLException {
		String insert = "INSERT INTO Books(Title, Author, Category_ID, Num_of_pages, Publisher, Language, Description, ISBN) " + 
						"VALUES(?, ?, ?, ?, ?, ?, ?, ?);";
		String log 	  = "INSERT INTO Log (User_ID, Book_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'INSERT');";
		
		PreparedStatement pstmtInsert = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtInsert.setString(1, book.getTitle());
		pstmtInsert.setString(2, book.getAuthor());
		pstmtInsert.setInt(3, categoryID);
		pstmtInsert.setInt(4, book.getnPages());
		pstmtInsert.setString(5, book.getPublisher());
		pstmtInsert.setString(6, book.getLanguage());
		pstmtInsert.setString(7, book.getDescription());
		pstmtInsert.setInt(8, book.getISBN());
		pstmtInsert.executeUpdate();
		
		ResultSet rs = pstmtInsert.getGeneratedKeys();
		int bookID = 0;
		if(rs.next())
			bookID = rs.getInt(1);
		else
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setInt(2, bookID);
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void updateBook(Book book, int categoryID, String userID) throws SQLException {
		String update = "UPDATE Books " + 
						"SET Title = ?, Author = ?, Category_ID = ?, Num_of_pages = ?, Publisher = ?," + 
						"Language = ?, Description = ?, ISBN = ? " +
						"WHERE Book_ID = ?;";
		String log 	  = "INSERT INTO Log (User_ID, Book_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'UPDATE');";
		
		PreparedStatement pstmtUpdate = conn.prepareStatement(update);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtUpdate.setString(1, book.getTitle());
		pstmtUpdate.setString(2, book.getAuthor());
		pstmtUpdate.setInt(3, categoryID);
		pstmtUpdate.setInt(4, book.getnPages());
		pstmtUpdate.setString(5, book.getPublisher());
		pstmtUpdate.setString(6, book.getLanguage());
		pstmtUpdate.setString(7, book.getDescription());
		pstmtUpdate.setInt(8, book.getISBN());
		pstmtUpdate.setInt(9, book.getBookID());
		int rowsAffected = pstmtUpdate.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setInt(2, book.getBookID());
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void deleteBook(int bookID, String userID) throws SQLException {
		String delete = "DELETE FROM Books " + 
						"WHERE Book_ID = ? ;";
		String log 	  = "INSERT INTO Log (User_ID, Book_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'DELETE');";
		
		PreparedStatement pstmtDelete = conn.prepareStatement(delete);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtDelete.setInt(1, bookID);
		int rowsAffected = pstmtDelete.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setInt(2, bookID);
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void insertNewCustomer(User customer, String userID) throws SQLException {
		String insert = "INSERT INTO Customer_Account " + 
						"VALUES(?, ?, ?, ?, ?, 'GREEN', 0);";
		String log 	  = "INSERT INTO Log (User_ID, Customer_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'INSERT');";
		
		PreparedStatement pstmtInsert = conn.prepareStatement(insert);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtInsert.setString(1, customer.getUsername());
		pstmtInsert.setString(2, customer.getPassword());
		pstmtInsert.setString(3, customer.getName());
		pstmtInsert.setString(4, customer.getSurname());
		pstmtInsert.setString(5, customer.getE_mail());
		int rowsAffected = pstmtInsert.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setString(2, customer.getUsername());
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void updateCustomer(User customer, String userID) throws SQLException {
		String update = "UPDATE Customer_Account " +
						"SET Username = ?, Password = ?, Name = ?, Surname = ?, E_Mail = ? " +
						"WHERE Username = ?;";
		String log 	  = "INSERT INTO Log (User_ID, Customer_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'UPDATE');";
		
		PreparedStatement pstmtUpdate = conn.prepareStatement(update);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtUpdate.setString(1, customer.getUsername());
		pstmtUpdate.setString(2, customer.getPassword());
		pstmtUpdate.setString(3, customer.getName());
		pstmtUpdate.setString(4, customer.getSurname());
		pstmtUpdate.setString(5, customer.getE_mail());
		pstmtUpdate.setString(6, customer.getUsername());
		int rowsAffected = pstmtUpdate.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setString(2, customer.getUsername());
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void deleteCustomer(String CustomerID, String userID) throws SQLException {
		String delete = "DELETE FROM Customer_Account " + 
						"WHERE Username = ?;";
		String log 	  = "INSERT INTO Log (User_ID, Customer_ID, Date, Update_type) VALUES (?, ?, DATE('now'), 'DELETE');";
		
		PreparedStatement pstmtDelete = conn.prepareStatement(delete);
		PreparedStatement pstmtLog = conn.prepareStatement(log);
		
		conn.setAutoCommit(false);
		
		pstmtDelete.setString(1, CustomerID);
		int rowsAffected = pstmtDelete.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		pstmtLog.setString(1, userID);
		pstmtLog.setString(2, CustomerID);
		pstmtLog.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public ResultSet getUserLentBooksList(String username) throws SQLException
	{
		String sql = "SELECT * FROM Books WHERE User_ID = ?;";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, username);
		ResultSet rs = pstmt.executeQuery();
		return rs;
	}
	
	public ResultSet getCategoryIDFromName(String categoryName) throws SQLException
	{
		String sql = "SELECT Category_ID FROM Category_Types WHERE Category = ?;";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, categoryName);
		ResultSet rs = pstmt.executeQuery();
		return rs;
	}
	
	public void updateCustomerLentBook(int bookID, String username, String deadlineDate) throws SQLException {
		String userLentBook = "UPDATE Books " +
							  "SET User_ID = ?, Collection_date = DATE('now'), Deadline_status = 'GREEN', Deadline_date = DATE(?) " +
							  "WHERE Book_ID = ?;";
		PreparedStatement pstmtUserLentBook = conn.prepareStatement(userLentBook);
		
		conn.setAutoCommit(false);
		
		pstmtUserLentBook.setString(1, username);
		pstmtUserLentBook.setString(2, deadlineDate);
		pstmtUserLentBook.setInt(3, bookID);
		int rowsAffected = pstmtUserLentBook.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void updateCustomerReturnedBook(int bookID) throws SQLException {
		String userReturnedBook = "UPDATE Books " +
				  				  "SET User_ID = NULL, Collection_date = NULL, Deadline_status = NULL, Deadline_date = NULL, Fine = NULL " +
				  				  "WHERE Book_ID = ?;";
		PreparedStatement pstmtUserReturnedBook = conn.prepareStatement(userReturnedBook);
		
		conn.setAutoCommit(false);
		pstmtUserReturnedBook.setInt(1, bookID);
		int rowsAffected = pstmtUserReturnedBook.executeUpdate();
		
		if(rowsAffected != 1)
			conn.rollback();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public ResultSet getPastExpiredBooksFromCustomer(String customer) throws SQLException {
		String customerPastExpiredBooks = "SELECT Expired_books FROM Customer_Expired_Books_Vault WHERE Username = ?;";
		PreparedStatement pstmtCustomerPastExpiredBooks = conn.prepareStatement(customerPastExpiredBooks);
		
		pstmtCustomerPastExpiredBooks.setString(1, customer);
		ResultSet rs = pstmtCustomerPastExpiredBooks.executeQuery();
		return rs;
	}
	
	public void setNewPastExpiredBooksForCustomer(String customer, int expiredBooks) throws SQLException {
		conn.setAutoCommit(false);
		String customerPastExpiredBooks = "INSERT INTO Customer_Expired_Books_Vault VALUES(?, ?)";
		PreparedStatement pstmtCustomerPastExpiredBooks = conn.prepareStatement(customerPastExpiredBooks);
		
		pstmtCustomerPastExpiredBooks.setString(1, customer);
		pstmtCustomerPastExpiredBooks.setInt(2, expiredBooks);
		pstmtCustomerPastExpiredBooks.executeUpdate();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void updatePastExpiredBooksForCustomer(String customer, int expiredBooks) throws SQLException {
		conn.setAutoCommit(false);
		String customerPastExpiredBooks = "UPDATE Customer_Expired_Books_Vault SET Expired_books = ? WHERE Username = ?;";
		PreparedStatement pstmtCustomerPastExpiredBooks = conn.prepareStatement(customerPastExpiredBooks);
		
		pstmtCustomerPastExpiredBooks.setInt(1, expiredBooks);
		pstmtCustomerPastExpiredBooks.setString(2, customer);
		pstmtCustomerPastExpiredBooks.executeUpdate();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public int getCurrentDate() throws SQLException {
		String currentDate = "SELECT DATE('now');";
		Statement stmtCurrentDate = conn.createStatement();
		ResultSet rs = stmtCurrentDate.executeQuery(currentDate);
		
		return rs.getInt(1);
	}

	public void setCustomerStatus(String customer, String newStatus) throws SQLException {
		conn.setAutoCommit(false);
		String customerNewStatus = "UPDATE Customer_Account SET User_deadline_status = ? WHERE Username = ?;";
		PreparedStatement pstmtCustomerNewStatus = conn.prepareStatement(customerNewStatus);
		
		pstmtCustomerNewStatus.setString(1, newStatus);
		pstmtCustomerNewStatus.setString(2, customer);
		pstmtCustomerNewStatus.executeUpdate();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public void setBookStatus(int bookID, String newStatus) throws SQLException {
		conn.setAutoCommit(false);
		String bookNewStatus = "UPDATE Books SET Deadline_status = ? WHERE Book_ID = ?;";
		PreparedStatement pstmtBookNewStatus = conn.prepareStatement(bookNewStatus);
		
		pstmtBookNewStatus.setString(1, newStatus);
		pstmtBookNewStatus.setInt(2, bookID);
		pstmtBookNewStatus.executeUpdate();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public ResultSet getMailInformationFromCustomer(String customer) throws SQLException {
		String customerMailInformation = "SELECT Title, Deadline_date, (JULIANDAY(Deadline_date) - JULIANDAY(DATE('now'))) AS Remaining_days, Fine " + 
										 "FROM Books " + 
										 "WHERE User_ID = ?;";
		PreparedStatement pstmtCustomerMailInformation = conn.prepareStatement(customerMailInformation);
		
		pstmtCustomerMailInformation.setString(1, customer);
		ResultSet rs = pstmtCustomerMailInformation.executeQuery();
		return rs;
	}
	
	public ResultSet getBooksInformation() throws SQLException {
		String booksInformation = "SELECT Book_ID, Deadline_status, (JULIANDAY(Deadline_date) - JULIANDAY(DATE('now'))) AS Remaining_days, Fine " + 
								  "FROM Books;";
		Statement stmtBooksInformation = conn.createStatement();
		ResultSet rs = stmtBooksInformation.executeQuery(booksInformation);
		return rs;
	}
	
	public void setBookFine(int bookID, int fine) throws SQLException {
		conn.setAutoCommit(false);
		String bookFine = "UPDATE Books SET Fine = ? WHERE Book_ID = ?;";
		PreparedStatement pstmtFine = conn.prepareStatement(bookFine);
		
		pstmtFine.setInt(1, fine);
		pstmtFine.setInt(2, bookID);
		pstmtFine.executeUpdate();
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	/*public ResultSet getBookFine(int bookID) throws SQLException {
		String bookFine = "SELECT Fine FROM Books WHERE Book_ID = ?;";
		PreparedStatement pstmtFine = conn.prepareStatement(bookFine);
		
		pstmtFine.setInt(1, bookID);
		ResultSet rs = pstmtFine.executeQuery();
		return rs;
	}*/
	
	public ResultSet getNextScheduledDeadlineCheck() throws SQLException {
		String nextCheck = "SELECT DATE(Scheduled_date) FROM Next_Scheduled_Deadline_Check;";
		Statement stmtNextCheck = conn.createStatement();
		
		ResultSet rs = stmtNextCheck.executeQuery(nextCheck);
		return rs;
	}
	
	public void setNextScheduledDeadlineCheck(Date nextSchedule) throws SQLException {
		conn.setAutoCommit(false);
		String nextCheck = "UPDATE Next_Scheduled_Deadline_Check SET Scheduled_date = ?;";
		PreparedStatement pstmtCheck = conn.prepareStatement(nextCheck);
		
		pstmtCheck.setString(1, new SimpleDateFormat("yyyy-MM-dd").format(nextSchedule));
		pstmtCheck.executeUpdate();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
}
