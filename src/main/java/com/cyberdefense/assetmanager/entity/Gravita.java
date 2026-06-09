package com.cyberdefense.assetmanager.entity;

// enum per i livelli di gravità di un ticket
// uso un enum così evito che qualcuno scriva valori a caso nel DB
public enum Gravita {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}