/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbc.htmlreader;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.htmlparser.Node;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.ParserException;

/**
 *
 * @author CClose
 */
public class HTMLReader {

    private Lexer       lr          = null;
    private Element     current     = null;
    private CellCreator cellCreator = null;

    public void setCellCreator(CellCreator cellCreator) {
        this.cellCreator = cellCreator;
    }

    public interface CellCreator {
        public String getValue(HTMLReader reader)  throws HTMLReaderException;
    }
    public class HTMLReaderException extends Exception {
        private static final long serialVersionUID = 1L;
        private String            message          = null;

        protected HTMLReaderException(String message) {
            this.message   = message;
        }
        public String getMessage() {
            return message;
        }
    }
    public HTMLReader(InputStream stream) throws HTMLReaderException {
        try {
            lr = new Lexer(new Page(stream, null));
        } catch (UnsupportedEncodingException ex) {
            throw new HTMLReaderException(ex.toString());
        }
    }
    public Element getCurrentToken() {
        return current;
    }
    public Element nextToken() throws HTMLReaderException {
        Node node = null;

        current = null;

        try {
            while ((node = lr.nextNode()) != null) {
                String html = node.toHtml(true).trim();

                if (html.length() != 0) {
                    current = new Element();

                    if (html.startsWith("</")) {
                        current.setType(Element.Type.End);
                        current.setText(html);
                    } else if (html.startsWith("<")) {
                        current.setType(Element.Type.Start);

                        int i = html.indexOf(' ');

                        if (i != -1) {
                            StringBuffer  id    = new StringBuffer();
                            StringBuffer  value = new StringBuffer();
                            boolean idSeek    = true;
                            boolean valueSeek = false;

                            current.setText(html.substring(1, i));
                            html = html.substring(i + 1);

                            if (html.endsWith("/>")) {
                                html = html.substring(0, html.length() - 2);
                                current.setOpen(false);
                            } else
                                html = html.substring(0, html.length() - 1);

                            html = html.trim();

                            for (i = 0; i < html.length(); i++) {
                                char ch = html.charAt(i);

                                if (valueSeek) {
                                    if (ch == '"') {
                                        current.addAttribute(id.toString(), value.toString());
                                        idSeek    = true;
                                        valueSeek = false;
                                        id.setLength(0);
                                        value.setLength(0);
                                    } else
                                        value.append(ch);

                                } else if (ch == '=') {
                                    idSeek = false;
                                } else if (ch == '"') {
                                    valueSeek = true;
                                } else
                                    id.append(ch);
                            }
                        }
                        else
                            current.setText(html);
                    } else {
                        current.setType(Element.Type.Text);
                        current.setText(html);
                    }

                    return current;
                }
            }
        } catch (ParserException ex) {
            throw new HTMLReaderException(ex.toString());
        }
        return null;
    }
    private Row getRow() throws HTMLReaderException {
        Row      row  = null;
        Row.Type type = null;
        
        while (current != null && !current.matchesStart("tr")) nextToken();

        if (current != null) nextToken();

        while (current != null && !current.matchesEnd("tr")) {
            if (type == null) {
                type = current.getText().equalsIgnoreCase("th")? Row.Type.Header : Row.Type.Data;
                row = new Row();
                row.setType(type);
            }
            if (type == Row.Type.Header) {
                if (!current.matchesStart("th")) throw new HTMLReaderException("Expected <th> found " + current.getText());
            } else if (!current.matchesStart("td"))
                throw new HTMLReaderException("Expected <td> found " + current.getText());
            nextToken();

            if (current == null) throw new HTMLReaderException("Table incomplete");
            if (cellCreator == null) {
                row.addColumn(current.getType() == Element.Type.Text? current.getText() : "");
            } else
                row.addColumn(cellCreator.getValue(this));

            if (current.getType() == Element.Type.End) nextToken();
        }
        return row;
    }
    public Table getTable() throws HTMLReaderException {
        Table table = new Table();
        Row   row   = null;
        
        while (current != null && !current.matchesStart("Table")) nextToken();
       
        while (current != null && !current.matchesEnd("Table")) {
            nextToken();
            row = getRow();
            
            if (row != null) table.addRow(row);
        }
        return table;
    }
}
