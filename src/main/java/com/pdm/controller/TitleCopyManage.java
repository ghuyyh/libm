package com.pdm.controller;

import com.pdm.dao.AuthorDAO;
import com.pdm.dao.BookCopyDAO;
import com.pdm.dao.BookTitleDAO;
import com.pdm.model.Author;
import com.pdm.model.BookCopy;
import com.pdm.model.BookTitle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    @FXML
    private Label title_count;

    private BookTitleDAO bookTitleDAO = new BookTitleDAO();
    private ObservableList<BookTitle> bookList = FXCollections.observableArrayList();
    private BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private ObservableList<BookCopy> copyList = FXCollections.observableArrayList();
    private AuthorDAO authorDAO = new AuthorDAO();
    private ObservableList<Author> authList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // book
        col_isbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("title"));
        col_year.setCellValueFactory(new PropertyValueFactory<>("publishYear"));
        col_publisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
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
        title_count.setText(bookList.size() + " Titles");

        book_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // load copy
                String selected_bookid = newSelection.getBookID();
                copyList.setAll(bookCopyDAO.get_copy(selected_bookid));
            } else {
                copyList.clear();
            }
        });

        book_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // load author
                String selected_bookid = newSelection.getBookID();
                authList.setAll(authorDAO.get_book_auth(selected_bookid));
            } else {
                authList.clear();
            }
        });

    }

    private void refreshBookTable() {
        bookList.setAll(bookTitleDAO.get_book());
    }

    private void refreshCopyTable() {
        copyList.setAll(bookCopyDAO.get_copy(""));
    }

    private void refreshAuthorTable() {
        authList.setAll(authorDAO.get_book_auth(""));
    }

    @FXML
    private void addBook() {

    }

    @FXML
    private void deleteBook() {

    }
}
