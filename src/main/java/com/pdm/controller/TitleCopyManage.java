package com.pdm.controller;

import java.time.Year;

import com.pdm.dao.AuthorDAO;
import com.pdm.dao.BookCopyDAO;
import com.pdm.dao.BookTitleDAO;
import com.pdm.model.Author;
import com.pdm.model.BookCopy;
import com.pdm.model.BookTitle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

//manage book
public class TitleCopyManage extends basecontrol {
    @FXML
    private TableView<BookTitle> book_table;
    @FXML
    private TableColumn<BookTitle, String> col_isbn;
    @FXML
    private TableColumn<BookTitle, String> col_title;
    @FXML
    private TableColumn<BookTitle, Integer> col_year;
    @FXML
    private TableColumn<BookTitle, String> col_publisher;
    @FXML
    private TableColumn<BookTitle, Integer> col_copies;

    // copy
    @FXML
    private TableView<BookCopy> copy_table;
    @FXML
    private TableColumn<BookCopy, String> col_copyid;
    @FXML
    private TableColumn<BookCopy, String> col_shelf;
    @FXML
    private TableColumn<BookCopy, String> col_status;
    @FXML
    private TableColumn<BookCopy, String> col_note;

    // author
    @FXML
    private TableView<Author> auth_table;
    @FXML
    private TableColumn<Author, String> col_authid;
    @FXML
    private TableColumn<Author, String> col_authname;
    @FXML
    private TableColumn<Author, String> col_bio;

    // add title
    @FXML
    private TextField isbn_field;
    @FXML
    private TextField title_field;
    @FXML
    private TextField year_field;
    @FXML
    private TextField publisherid_field;

    @FXML
    private Label title_count;
    @FXML
    private CheckBox show_available_copies_checkbox;
    @FXML
    private TextField search_field;

    private BookTitleDAO bookTitleDAO = new BookTitleDAO();
    private ObservableList<BookTitle> bookList = FXCollections.observableArrayList();
    private BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private ObservableList<BookCopy> copyList = FXCollections.observableArrayList();
    private AuthorDAO authorDAO = new AuthorDAO();
    private ObservableList<Author> authList = FXCollections.observableArrayList();
    private ObservableList<BookTitle> allbook = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // book
        col_isbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("title"));
        col_year.setCellValueFactory(new PropertyValueFactory<>("publishYear"));
        col_publisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        col_copies.setCellValueFactory(new PropertyValueFactory<>("available_copies_count"));
        // copy
        col_copyid.setCellValueFactory(new PropertyValueFactory<>("Copy_ID"));
        col_shelf.setCellValueFactory(new PropertyValueFactory<>("Shelf_Location"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("Status"));
        col_note.setCellValueFactory(new PropertyValueFactory<>("note"));
        // author
        col_authid.setCellValueFactory(new PropertyValueFactory<>("Author_ID"));
        col_authname.setCellValueFactory(new PropertyValueFactory<>("Author_Name"));
        col_bio.setCellValueFactory(new PropertyValueFactory<>("Biography"));

        book_table.setItems(bookList);
        copy_table.setItems(copyList);
        auth_table.setItems(authList);

        refreshBookTable();
        allbook.setAll(bookList);
        title_count.setText(bookList.size() + " Titles");

        book_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // load copy
                String selected_bookid = newSelection.getBookID();
                refreshAuthorTable(selected_bookid);
                refreshCopyTable(selected_bookid);

            } else {
                copyList.clear();
                authList.clear();
            }
        });

        search_field.setOnAction(event -> {
            System.out.println("search:" + search_field.getText());
            String searchText = search_field.getText();
            if (searchText.isEmpty()) {
                refreshBookTable();
            } else {
                bookList.setAll(bookTitleDAO.search_book(searchText));
                title_count.setText(bookList.size() + " Titles");
            }

        });

    }

    private void refreshBookTable() {
        bookList.setAll(bookTitleDAO.get_book());
    }

    private void refreshCopyTable(String book_id) {
        if (show_available_copies_checkbox.isSelected()) {
            copyList.setAll(bookCopyDAO.get_title_available_copy(book_id));
        } else {
            copyList.setAll(bookCopyDAO.get_copy(book_id));
        }
    }

    private void refreshAuthorTable(String book_id) {
        authList.setAll(authorDAO.get_book_auth(book_id));
    }

    @FXML
    private void addBook() {
        String error = "";
        // boolean has_error = false;
        String isbn = isbn_field.getText();
        if (isbn.isEmpty()) {
            error = "ISBN cannot be empty.";
            showError(error);
            return;
        }
        if (!isbn.matches("\\d{3}-\\d{10}")) {
            error = "ISBN must be a 13 digit number in the format XXX-XXXXXXXXX.";
            showError(error);
            return;
        }
        // check if isbn exist
        if (allbook.stream().anyMatch(book -> book.getIsbn().equals(isbn))) {
            error = "A book with this ISBN already exists.";
            showError(error);
            return;
        }

        String title = title_field.getText();
        if (title.isEmpty() || title.length() > 255) {
            error = "Title cannot be empty and must be less than 255 characters.";
            showError(error);
            return;
        }

        if (year_field.getText().isEmpty()) {
            error = "Publish year cannot be empty.";
            showError(error);
            return;
        }
        if (!year_field.getText().matches("\\d{4}")) {
            error = "Publish year must be a 4 digit number.";
            showError(error);
            return;
        }
        int publishyear = Integer.parseInt(year_field.getText());
        if (publishyear < 1000 || publishyear > Year.now().getValue()) {
            error = "Publish year must be between 1000 and the current year.";
            showError(error);
            return;
        }

        String publisherID = publisherid_field.getText();
        if (publisherID.isEmpty() || publisherID.length() > 20) {
            error = "Publisher ID cannot be empty and must be less than 20 characters.";
            showError(error);
            return;
        }
        // check if publisher exist
        if (!allbook.stream().anyMatch(book -> book.getPublisher().equals(publisherID))) {
            error = "A publisher with this ID does not exist. Please add the publisher first.";
            showError(error);
            return;
        }

        boolean success = bookTitleDAO.addBook(isbn, title, publisherID, publishyear);
        if (success) {
            allbook = FXCollections.observableArrayList(bookTitleDAO.get_book());
            refreshBookTable();
            title_count.setText(bookList.size() + " Titles");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Book added successfully.");
            alert.show();
        } else {
            error = "Failed to add book.";
            showError(error);
        }

    }

    @FXML
    private void showAvailableCopy() {

        BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            String selected_bookid = selectedBook.getBookID();
            if (show_available_copies_checkbox.isSelected()) {
                copyList.setAll(bookCopyDAO.get_title_available_copy(selected_bookid));
            } else {
                refreshCopyTable(selected_bookid);
            }
        } else {
            showError("Please select a book title first.");
        }
    }

    @FXML
    private void deleteBook() {

    }

    @FXML
    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }

}
