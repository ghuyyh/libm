package com.pdm.controller;

import java.util.List;

import com.pdm.dao.AuthorDAO;
import com.pdm.dao.BookTitleDAO;
import com.pdm.dao.MemberDAO;
import com.pdm.model.Author;
import com.pdm.model.BookTitle;
import com.pdm.model.MemCat;
import com.pdm.model.Member;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

public class ReportManage extends basecontrol {

    @FXML
    private TableView<BookTitle> top10_table;
    @FXML
    private TableColumn<BookTitle, String> col_bookid;
    @FXML
    private TableColumn<BookTitle, String> col_title;
    @FXML
    private TableColumn<BookTitle, Integer> col_count;

    @FXML
    private TableView<MemCat> cat_unpaid_fine_table;
    @FXML
    private TableColumn<MemCat, String> col_cat;
    @FXML
    private TableColumn<MemCat, Long> col_finepercat;

    @FXML
    TableView<Member> mem_table;
    @FXML
    TableColumn<Member, String> col_memid;
    @FXML
    TableColumn<Member, String> col_memname;

    @FXML
    private ComboBox<Author> author_field;
    private ObservableList<Author> allAuthors = FXCollections.observableArrayList();;
    private FilteredList<Author> filteredAuthors;
    private AuthorDAO authorDAO = new AuthorDAO();

    private BookTitleDAO bookTitleDAO = new BookTitleDAO();
    private ObservableList<BookTitle> top10list = FXCollections.observableArrayList();

    private ObservableList<MemCat> unpaidFineList = FXCollections.observableArrayList();
    private MemberDAO memberDAO = new MemberDAO();

    @FXML
    public void initialize() {
        // top 10 most borrow books this year
        col_bookid.setCellValueFactory(new PropertyValueFactory<>("bookID"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("title"));
        col_count.setCellValueFactory(new PropertyValueFactory<>("borrowed_thisyear_count"));
        top10list.addAll(bookTitleDAO.get_top10_borrowed_thisyear(""));

        // total oustanding fine per member category
        col_cat.setCellValueFactory(new PropertyValueFactory<>("Type_Name"));
        col_finepercat.setCellValueFactory(new PropertyValueFactory<>("Total_Unpaid_Fine"));
        unpaidFineList.addAll(memberDAO.unpaid_fine_per_mem_cat());

        // member who read all book from agiven auhtor
        // members who read all books from a given author
        col_memid.setCellValueFactory(new PropertyValueFactory<>("Member_ID"));
        col_memname.setCellValueFactory(new PropertyValueFactory<>("Full_Name"));
        List<Author> authors = authorDAO.get_all_auth();
        allAuthors = FXCollections.observableArrayList(authors);
        filteredAuthors = new FilteredList<>(allAuthors, p -> true);
        author_field.setItems(filteredAuthors);

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

        author_field.valueProperty().addListener((obs, oldAuthor, selectedAuthor) -> {
            if (selectedAuthor != null)
                refreshMemTable(selectedAuthor.getAuthor_ID(), selectedAuthor.getAuthor_Name());
        });

        cat_unpaid_fine_table.setItems(unpaidFineList);
        top10_table.setItems(top10list);
    }

    private void refreshMemTable(String authorID, String authorName) {
        List<Member> list = authorDAO.get_mem_who_read_all_book_from_author(authorID, authorName);
        ObservableList<Member> observableMemList = FXCollections.observableArrayList(list);
        mem_table.setItems(observableMemList);
    }

}