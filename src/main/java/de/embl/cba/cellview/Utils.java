package de.embl.cba.cellview;

import java.time.LocalDate;
import java.time.LocalTime;

public class Utils
{
	public static String getLocalDateAndHourAndMinute()
	{
		final LocalDate localDate = LocalDate.now();
		final LocalTime localTime = LocalTime.now();
		String localDateAndTime = localDate + "-" + localTime.getHour() + "-" + localTime.getMinute();
		return localDateAndTime;
	}
}
