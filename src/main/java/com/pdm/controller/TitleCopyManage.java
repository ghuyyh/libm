package com.pdm.controller;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import com.pdm.dao.AuthorDAO;
import com.pdm.dao.BookCopyDAO;
import com.pdm.dao.BookTitleDAO;
import com.pdm.model.Author;
import com.pdm.model.BookCopy;
import com.pdm.model.BookTitle;
import com.pdm.model.Publisher;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

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

    // addcopy
    @FXML
    private TextField shelf_field;
    @FXML
    private ChoiceBox<String> copy_status_choice;
    @FXML
    private TextField copy_note_field;

    // update title
    @FXML
    private TextField u_isbn_field;
    @FXML
    private TextField u_title_field;
    @FXML
    private TextField u_year_field;
    @FXML
    private TextField u_publisherid_field;

    // publisher
    @FXML
    private TableView<Publisher> publisher_table;
    @FXML
    private TableColumn<Publisher, String> col_publisherid;
    @FXML
    private TableColumn<Publisher, String> col_publishername;
    @FXML
    private TableColumn<Publisher, String> col_addr;
    @FXML
    private TableColumn<Publisher, String> col_email;

    // add publisher
    @FXML
    private TextField publisher_name_field;
    @FXML
    private TextField publisher_address_field;
    @FXML
    private TextField publisher_email_field;

    // combobox
    @FXML
    private ComboBox<Author> author_field;
    @FXML
    private TextField author_name_field;
    @FXML
    private TextField author_bio_field;
    private ObservableList<Author> allAuthors = FXCollections.observableArrayList();;
    private FilteredList<Author> filteredAuthors;

    @FXML
    private Label title_count;
    @FXML
    private CheckBox show_available_copies_checkbox;
    @FXML
    private CheckBox use_exist_author_checkbox;
    @FXML
    private TextField search_field;

    private BookTitleDAO bookTitleDAO = new BookTitleDAO();
    private ObservableList<BookTitle> bookList = FXCollections.observableArrayList();
    private BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private ObservableList<BookCopy> copyList = FXCollections.observableArrayList();
    private AuthorDAO authorDAO = new AuthorDAO();
    private ObservableList<Author> authList = FXCollections.observableArrayList();
    private ObservableList<BookTitle> allbook = FXCollections.observableArrayList();
    private ObservableList<Publisher> publisherList = FXCollections.observableArrayList();

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
        // publisher
        col_publisherid.setCellValueFactory(new PropertyValueFactory<>("Publisher_ID"));
        col_publishername.setCellValueFactory(new PropertyValueFactory<>("Publisher_Name"));
        col_addr.setCellValueFactory(new PropertyValueFactory<>("Address"));
        col_email.setCellValueFactory(new PropertyValueFactory<>("Contact_Email"));

        book_table.setItems(bookList);
        copy_table.setItems(copyList);
        auth_table.setItems(authList);
        publisher_table.setItems(publisherList);

        refreshBookTable();
        refreshPublisherTable();
        allbook.setAll(bookList);
        // tong so sach
        title_count.setText(bookList.size() + " Titles");
        // choicebox copy status
        copy_status_choice
                .setItems(FXCollections.observableArrayList("Available", "Borrowed", "Reserved", "Damaged", "Lost"));
        copy_status_choice.setValue("Available");
        List<Author> authors = authorDAO.get_all_auth();
        // combobox
        allAuthors = FXCollections.observableArrayList(authors);
        filteredAuthors = new FilteredList<>(allAuthors, p -> true);
        author_field.setItems(filteredAuthors);

        book_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // load copy
                String selected_bookid = newSelection.getBookID();
                refreshAuthorTable(selected_bookid);
                refreshCopyTable(selected_bookid);
                refreshUpdateFields(newSelection);

            } else {
                copyList.clear();
                authList.clear();
            }
        });

        search_field.setOnAction(event -> { //search book
            System.out.println("search:" + search_field.getText());
            String searchText = search_field.getText();
            if (searchText.isEmpty()) {
                refreshBookTable();
            } else {
                bookList.setAll(bookTitleDAO.search_book(searchText));
                title_count.setText(bookList.size() + " Titles");
            }

        });

        author_field.setConverter(new StringConverter<Author>() {
            @Override
            public String toString(Author author) {
                if (author == null)
                    return "";
                return author.getAuthor_ID() + " - " + author.getAuthor_Name();
            }

            @Override
            public Author fromString(String string) {
                if (string == null || string.isBlank())
                    return null;
                String value = string.trim();
                for (Author author : allAuthors) {
                    if (toString(author).equalsIgnoreCase(value) || author.getAuthor_ID().equalsIgnoreCase(value)) {
                        return author;
                    }
                }
                return null;
            }
        });

        author_field.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (!newValue.equals(author_field.getEditor().getText()))
                    return;

                Author selectedAuthor = author_field.getValue();
                if (selectedAuthor != null
                        && author_field.getConverter().toString(selectedAuthor).equalsIgnoreCase(newValue.trim()))
                    return;

                if (newValue.isBlank()) {
                    filteredAuthors.setPredicate(author -> true);
                } else {
                    String keyword = newValue.toLowerCase().trim();
                    filteredAuthors.setPredicate(author -> author.getAuthor_ID().toLowerCase().contains(keyword)
                            || author.getAuthor_Name().toLowerCase().contains(keyword));
                }

                if (!newValue.isBlank() && !author_field.isShowing())
                    author_field.show();
            });
        });

    }

    private void refreshBookTable() {
        bookList.setAll(bookTitleDAO.get_book());
        title_count.setText(bookList.size() + " Titles");
        allbook.setAll(bookList);
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

    private void refreshPublisherTable() {
        publisherList.setAll(bookTitleDAO.get_publishers());
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
        if (!publisherList.stream().anyMatch(publisher -> publisher.getPublisher_ID().equals(publisherID))) {
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
    private void updateBook() {
        BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showError("Please select a book title to update.");
            return;
        }

        String isbn = u_isbn_field.getText();
        if (isbn.isEmpty()) {
            showError("ISBN cannot be empty.");
            return;
        }
        if (!isbn.matches("\\d{3}-\\d{10}")) {
            showError("ISBN must be a 13 digit number in the format XXX-XXXXXXXXX.");
            return;
        }
        // check if isbn exist
        // if (allbook.stream().anyMatch(book -> book.getIsbn().equals(isbn))) {
        // showError("A book with this ISBN already exists.");
        // return;
        // }

        String title = u_title_field.getText();
        if (title.isEmpty() || title.length() > 255) {
            showError("Title cannot be empty and must be less than 255 characters.");
            return;
        }

        if (u_year_field.getText().isEmpty()) {
            showError("Publish year cannot be empty.");
            return;
        }
        if (!u_year_field.getText().matches("\\d{4}")) {
            showError("Publish year must be a 4 digit number.");
            return;
        }
        int publishyear = Integer.parseInt(u_year_field.getText());
        if (publishyear < 1000 || publishyear > Year.now().getValue()) {
            showError("Publish year must be between 1000 and the current year.");
            return;
        }

        String publisherID = u_publisherid_field.getText();
        if (publisherID.isEmpty() || publisherID.length() > 20) {
            showError("Publisher ID cannot be empty and must be less than 20 characters.");
            return;
        }
        // check if publisher exist
        if (!publisherList.stream().anyMatch(publisher -> publisher.getPublisher_ID().equals(publisherID))) {
            showError("A publisher with this ID does not exist. Please add the publisher first.");
            return;
        }
        boolean success = bookTitleDAO.updateBook(selectedBook.getBookID(), isbn, title, publisherID, publishyear);
        if (success) {
            refreshBookTable();
            title_count.setText(bookList.size() + " Titles");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Book updated successfully.");
            alert.show();
        } else {
            showError("Failed to update book.");
        }

    }

    private void refreshUpdateFields(BookTitle book) {

        u_isbn_field.setText(book.getIsbn());
        System.out.println(book.getTitle());
        u_title_field.setText(book.getTitle());
        u_publisherid_field.setText(book.getPublisherID());
        u_year_field.setText(String.valueOf(book.getPublishYear()));
    }

    @FXML
    private void deleteBook() {
        BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showError("Please select a book title to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Sure to delete this selected book title?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("ok delete");
        } else {
            System.out.println("not delete");
            return;
        }

        String error = bookTitleDAO.BookDeleteChecker(selectedBook.getBookID());
        if (error != null) {
            showError(error + ", cannot delete book.");
            return;
        }

        boolean success = bookTitleDAO.deleteBook(selectedBook.getBookID());
        if (success) {
            refreshBookTable();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Book deleted successfully.");
            alert.show();
        } else {
            showError("cannot delete book. It may have copies, reservations, or loan history.");
        }

    }

    @FXML
    private void addCopy() {
        BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showError("Please select a book title to add a copy.");
            return;
        }
        if (shelf_field.getText().isEmpty() || shelf_field.getText().length() > 100) {
            showError("Shelf location cannot be empty and must be less than 100 characters.");
            return;
        }

        bookCopyDAO.addCopy(
                selectedBook.getBookID(),
                shelf_field.getText(),
                copy_status_choice.getValue(),
                copy_note_field.getText());
        refreshCopyTable(selectedBook.getBookID());

    }

    @FXML
    private void deleteCopy() {
        BookCopy selectedCopy = copy_table.getSelectionModel().getSelectedItem();
        if (selectedCopy == null) {
            showError("Select a copy first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Sure to delete this selected copy?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("ok delete");
        } else {
            System.out.println("not delete");
            return;
        }
        String error = bookCopyDAO.CopyDeleteChecker(selectedCopy.getBook_ID(), selectedCopy.getCopy_ID());
        if (error != null) {
            showError(error + ", cannot delete copy.");
            return;
        }

        boolean success = bookCopyDAO.deleteCopy(selectedCopy.getBook_ID(), selectedCopy.getCopy_ID());
        if (success) {
            BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                refreshCopyTable(selectedBook.getBookID());
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Copy deleted successfully.");
            alert.show();
        } else {
            showError("cannot delete copy. It may have loan history or be on loan.");
        }
    }

    @FXML
    private void addPublisher() {

        if (publisher_name_field.getText().isEmpty() || publisher_name_field.getText().length() > 150) {
            showError("Publisher name cannot be empty and must be less than 150 characters.");
            return;
        }
        String publisherName = publisher_name_field.getText();
        if (publisher_address_field.getText().length() > 255) {
            showError("Address must be less than 255 characters.");
            return;
        }
        String address = publisher_address_field.getText();
        if (publisher_email_field.getText().length() > 100) {
            showError("Contact email must be less than 100 characters.");
            return;
        }
        if (!publisher_email_field.getText().isEmpty() && !publisher_email_field.getText().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            showError("Please enter a valid contact email.");
            return;
        }
        String contactEmail = publisher_email_field.getText();
        if (!bookTitleDAO.addPublisher(publisherName, address, contactEmail)) {
            showError("Failed to add publisher. The name or contact email may already exist.");
            return;
        }

        refreshPublisherTable();
        publisher_name_field.clear();
        publisher_address_field.clear();
        publisher_email_field.clear();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Publisher added successfully.");
        alert.show();
    }

    @FXML
    private void addAuthorToBook() {
        BookTitle selectedBook = book_table.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showError("Please select a book title to add an author.");
            return;
        }

        String authorID;
        if (use_exist_author_checkbox.isSelected()) {
            Author selectedAuthor = author_field.getValue();
            if (selectedAuthor == null) {
                showError("Please select an existing author.");
                return;
            }
            authorID = selectedAuthor.getAuthor_ID();
        } else {
            String authorName = author_name_field.getText().trim();
            String biography = author_bio_field.getText().trim();
            if (authorName.isEmpty() || authorName.length() > 150) {
                showError("Author name cannot be empty and must be less than 150 characters.");
                return;
            }

            authorID = authorDAO.generateAuthorID();
            if (!authorDAO.addAuthor(authorName, biography)) {
                showError("Failed to add author.");
                return;
            }
        }

        if (!authorDAO.addAuthorToBook(selectedBook.getBookID(), authorID)) {
            showError("Failed to add author to book.");
            return;
        }

        refreshAuthorTable(selectedBook.getBookID());
        allAuthors.setAll(authorDAO.get_all_auth());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Author add to book successfully.");
        alert.show();
    }

    @FXML
    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }

}
