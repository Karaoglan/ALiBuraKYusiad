package model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class ComputationRequestInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6498474517490692156L;
	private Date date;
	private String term;
	private String ergebnis;
	private String node;
	private String errorMessage="";
	public ComputationRequestInfo(Date date, String term, String ergebnis,
			String node) {
		this.date = date;
		this.term = term;
		this.ergebnis = ergebnis;
		this.node = node;
	}
	
	public ComputationRequestInfo(String message){
		this.errorMessage=message;
	}
	public Date getDate() {
		return date;
	}
	public String getTerm() {
		return term;
	}
	public String getErgebnis() {
		return ergebnis;
	}
	public String getNode() {
		return node;
	}
	@Override
	public String toString() {
		if(!errorMessage.isEmpty()){
			return errorMessage;
		}
		DateFormat dt=new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		return  dt.format(date) +"["+node+"]: "+ term +" = " + ergebnis ;
	}
	

		

	
	
}
