package acr.browser.barebones.databases;

public class HistoryItem {
	

		//private variables
        private int _id;
		private String _url;
		private String _title;

		// Empty constructor
		public HistoryItem(){

		}
		// constructor
		public HistoryItem(int id, String url, String title){
			this._id = id;
			this._url = url;
			this._title = title;
		}

		// constructor
		public HistoryItem(String url, String title){
			this._url = url;
			this._title = title;
		}
		// getting ID
		public int getID(){
			return this._id;
		}

		// setting id
		public void setID(int id){
			this._id = id;
		}

		// getting name
		public String getUrl(){
			return this._url;
		}

		// setting name
		public void setUrl(String url){
			this._url = url;
		}

		// getting phone number
		public String getTitle(){
			return this._title;
		}

		// setting phone number
		public void setTitle(String title){
			this._title = title;
		}
		
}