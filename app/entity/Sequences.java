package entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 
 * @author Sildu
 *
 */
@Entity
@Table(name = "SEQUENCES")
public class Sequences implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "SEQ_NAME")
	private String	seqName;
	@Column(name = "SEQ_NUMBER")
	private long	seqNumber;

	public String getSeqName() {
		return seqName;
	}

	public void setSeqName(String seqName) {
		this.seqName = seqName;
	}

	public long getSeqNumber() {
		return seqNumber;
	}

	public void setSeqNumber(long seqNumber) {
		this.seqNumber = seqNumber;
	}

}
