package processor;

import java.util.Objects;

public class Caption {

	private final int number;
	private final String timing;
	private String content;
	
	public Caption(int number, String timing, String content) {
		this.number = number;
		this.timing = timing;
		this.content = content;
	}
	
	public int getNumber() {
		return number;
	}
	public String getTiming() {
		return timing;
	}
	public String getContent() {
		return content;
	}
		
	protected void setContent(String content) {
		this.content = content;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Caption other = (Caption) obj;
		return Objects.equals(content, other.content) && number == other.number && Objects.equals(timing, other.timing);
	}
	
	public String toString() {
		return number + "\n" + timing + "\n" + content;
	}
	
}
