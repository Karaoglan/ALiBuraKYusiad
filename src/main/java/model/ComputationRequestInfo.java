package model;

import java.util.Date;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class ComputationRequestInfo {
	private Date date;
	private String term;
	private String ergebnis;
	private String node;
	public ComputationRequestInfo(Date date, String term, String ergebnis,
			String node) {
		this.date = date;
		this.term = term;
		this.ergebnis = ergebnis;
		this.node = node;
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
		return "ComputationRequestInfo [date=" + date + ", term=" + term
				+ ", ergebnis=" + ergebnis + ", node=" + node + "]";
	}
	

		

	
	
}
