package playground.dhosse.prt.scheduler;

import java.util.List;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.schedule.*;

public class NPersonsPickupDriveTask extends TaxiDriveTask{

	List<TaxiRequest> requests;
	
	public NPersonsPickupDriveTask(VrpPathWithTravelData path, List<TaxiRequest> requests) {
		
		super(path);
		this.requests = requests;
	}
	
    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DRIVE_EMPTY;
    }

    public List<TaxiRequest> getRequests()
    {
        return requests;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
