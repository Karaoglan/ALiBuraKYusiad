package exceptions;

public class ConnectionRollbackedException extends Exception {
	

	public ConnectionRollbackedException(){
		super();
	}
	
	public ConnectionRollbackedException(String message){
		super(message);
	}

}
