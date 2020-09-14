package develop;

import java.time.LocalDate;
import java.time.LocalTime;

public class DevelopLocalDateAndTime
{
	public static void main( String[] args )
	{
		final LocalDate localDate = LocalDate.now();
		final LocalTime localTime = LocalTime.now();

		String localDateAndTime = localDate + "-" + localTime.getHour() + "-" + localTime.getMinute();
	}
}
