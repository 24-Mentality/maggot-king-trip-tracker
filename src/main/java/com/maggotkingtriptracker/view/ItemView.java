package com.maggotkingtriptracker.view;

import lombok.Value;

@Value
public class ItemView
{
	int itemId;
	String name;
	long quantity;
	long totalValue;
	boolean perDose;
	boolean unique;
	boolean pending;
}
