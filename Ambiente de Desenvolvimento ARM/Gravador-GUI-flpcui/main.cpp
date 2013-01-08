/******************************************************************************

Project:           Cross-Platform GUI for lpc21isp, a command line ISP for
                   NXP LPC1000 / LPC2000 family and Analog Devices ADUC70xx
                   * This program has lpc21isp compiled into it.

Filename:          main.cpp

Compiler:          Microsoft VC++ 2003+
                   GCC Cygwin, GCC Linux

Author:            Moses McKnight (moses@texband.net)

Copyright:         (c) Moses McKnight 2011, All rights reserved

    This file is part of flpcui.

    flpcui is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    flpcui is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    and GNU General Public License along with flpcui.
    If not, see <http://www.gnu.org/licenses/>.
*/

#include <sys/types.h>

#if defined(_WIN32)     //Windows
#include <string.h>
#include <tchar.h>
#else                   //Not Windows
#include <dirent.h>
#endif

#include <errno.h>
#include <stdlib.h>
#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include <cctype>
#include "threads.h"
 extern "C" {
#include "lpc21isp/lpc21isp.h"
#include "lpc21isp/lpcprog.h"
#include "lpc21isp/adprog.h"
}
#include <FL/Fl_File_Icon.H>
#include <FL/Fl_Native_File_Chooser.H>
#include "flpcui.h"


lpcprog_ui *ui;
Fl_Text_Buffer *dbg_buff;  // text buffer
bool programming = false;
bool quit_programming = false;

Fl_Thread program_thread;

void* program_func(void* p);
void get_ports(void);

// Make Esc key stop any programming
int handle(int e)
{
    //if (FL_SHORTCUT == e)
    if (programming && (e == FL_SHORTCUT) && (Fl::event_key()==FL_Escape))
    {
        quit_programming = true;
    }
    return (e == FL_SHORTCUT); // eat all keystrokes
}

int main(int argc, char **argv)
{
    //Fl::scheme("gtk+");
    Fl::add_handler(handle);
    Fl_File_Icon::load_system_icons();
    ui = new lpcprog_ui();
    dbg_buff = new Fl_Text_Buffer();
    ui->dbg_text->buffer(dbg_buff);

    get_ports();

    ui->show(argc, argv);
    Fl::lock();
    int ret = Fl::run();

    delete dbg_buff;
    delete ui;
    return ret;
}

void on_open_file(Fl_Button*, void*)
{
    Fl_Native_File_Chooser fnfc;
    fnfc.title("Choose a file");
    fnfc.type(Fl_Native_File_Chooser::BROWSE_FILE);
    fnfc.filter("Hex Files\t*.hex\n"
                "Binary Files\t*.bin");
    fnfc.directory(ui->default_dir.c_str());  //"/home/moses");           // default directory to use
    // Show native chooser
    switch ( fnfc.show() )
    {
    case -1:
        AppDebugPrintf(2, "ERROR: %s\n", fnfc.errmsg());  // ERROR
        break;
    case  1:
        break;
    default:
        ui->file_input->value(fnfc.filename());
        std::string tmp(fnfc.filename());
        ui->default_dir = tmp.substr(0, tmp.find_last_of("/\\"));
        break;
    }
}

void on_program(Fl_Button*, void*)
{
    fl_create_thread(program_thread, program_func, 0);
}

void* program_func(void* p)
{
    dbg_buff->text("");     // Empty debug buffer before each programming session
    bool err = false;
    if ("" == ui->file_input->value())
    {
        AppDebugPrintf(0, "You must select a file to program!\n");
        err = true;
    }
    if (ui->port_choice->value() < 0)
    {
        AppDebugPrintf(0, "You must select a serial port!\n"); //port_choice->value() = %d", ui->port_choice->value());
        err = true;
    }
    if ("" == ui->osc_speed_input->value())
    {
        AppDebugPrintf(0, "You must enter an oscillator speed!\n");
        err = true;
    }
    if (true == err)
        return 0;

//DebugPrintf(1, "Syntax:  lpc21isp [Options] file[ file[ ...]] comport baudrate Oscillator_in_kHz\n\n"
//               "Example: lpc21isp test.hex com1 115200 14746\n\n"

    std::vector<const char*> arguments;
    std::stringstream tmp;

    arguments.push_back("flpcui ");
    if ("NXP" == ui->chip_choice->text()) //value())
        arguments.push_back("-NXPARM");
    else
        arguments.push_back("-ADARM");
    if (ui->erase_check->value())
        arguments.push_back("-wipe");
    if (ui->verify_check->value())
        arguments.push_back("-verify");
    if (ui->control_check->value())
        arguments.push_back("-control");
    if (ui->swap_check->value())
        arguments.push_back("-controlswap");
    if (ui->invert_check->value())
        arguments.push_back("-controlinv");
    if (ui->hdup_check->value())
        arguments.push_back("-halfduplex");

    //debug_choice->value() returns the index of the selected item, which in this case is the same as the debug level
    tmp << "-debug" << ui->debug_choice->value();
    std::string dbg = tmp.str();
    arguments.push_back(dbg.c_str());
    tmp.str("");
    tmp.clear();

    std::string ext(ui->file_input->value());
    ext = ext.substr(ext.find_last_of("."));
    if (ext == ".hex")
        arguments.push_back("-hex");
    else if (ext == ".bin")
        arguments.push_back("-bin");

    arguments.push_back(ui->file_input->value());

    std::string port("");
#if defined(_WIN32)  // Windows
    port = "\\\\.\\";
    port += ui->port_choice->text();
#else                // Linux (and MacOS maybe?)
    port = "/dev/";
    port += ui->port_choice->text();
#endif
    arguments.push_back(port.c_str());

    arguments.push_back(ui->baud_choice->text());
    arguments.push_back(ui->osc_speed_input->value());
    arguments.push_back(0);
    char** argv = (char**)(&arguments[0]);

	try
	{
	    int argc = (int)(arguments.size()-1);
	    if (debug_level > 3)
        {
            for (int i = 0; i < argc; i++)
                AppDebugPrintf(0, "%s ", argv[i]);
            AppDebugPrintf(0, "\n");
        }
        programming = true;
        AppDoProgram(argc, argv);
	}
	catch(int e)
	{
        AppDebugPrintf(0, "\nError number %d occured!\n", e);
    }
    AppDebugPrintf(0, "Done\n");
    programming = false;
    return 0;
}

void AppDebugPrintf(int level, const char *fmt, ...)
{
    va_list ap;

    if (level <= debug_level)
    {
        char pTemp[2000];
        va_start(ap, fmt);
        vsprintf(pTemp, fmt, ap);
        Fl::lock();
        ui->dbg_text->insert(pTemp);
        Fl::unlock();
        Fl::awake();  // Must call this so fltk redraws text_display.  This call is thread safe - FL::check() is not.
        va_end(ap);
    }
}

//extern "C" void AppException(int exception_level)
void AppException(int exception_level)
{
    throw exception_level;
}

int AppSyncing(int trials)
{
    if (quit_programming)
    {
        quit_programming = false;
        return 0;
    }
    if (trials > 15)  //Cancel sync after x tries
        return 0;
	return 1;
}

void AppWritten(int size)
{
}

void get_ports()
{
#if defined(_WIN32)  // Windows
    //Iterate through all 255 possible COM ports.
    for (UINT i=1; i<256; i++)
    {
        std::stringstream sPort;
        sPort << "\\\\.\\COM" << i;

        //Try to open the port
        bool bSuccess = false;
        HANDLE hPort = ::CreateFile(sPort.str().c_str(), GENERIC_READ | GENERIC_WRITE, 0, 0, OPEN_EXISTING, 0, 0);
        if (hPort == INVALID_HANDLE_VALUE)
        {
            DWORD dwError = GetLastError();
            //Check to see if the error was because another app already had the port open or a general failure
            if (dwError == ERROR_ACCESS_DENIED || dwError == ERROR_GEN_FAILURE || dwError == ERROR_SHARING_VIOLATION || dwError == ERROR_SEM_TIMEOUT)
                bSuccess = true;	//Port is there even though we couldn't open it
        }
        else
        {
            //The port was opened successfully so it must be there :)
            bSuccess = true;
            CloseHandle(hPort);
        }

        // Add port name to port_choice dropdown box
        if (bSuccess)
        {
            sPort.str("");
            sPort.clear();
            sPort << "COM" << i;
            ui->port_choice->add(sPort.str().c_str());
        }
    }
#else                // Linux (and MacOS maybe?)
    DIR *dp;
    struct dirent *dirp;
    if((dp = opendir("/dev")) == NULL)
    {
        return;
    }

    while ((dirp = readdir(dp)) != NULL)
    {
        std::string tmp(dirp->d_name);
        if ((tmp.substr(0, 4) == "ttyS") || (tmp.substr(0, 6) == "ttyACM") ||
            (tmp.substr(0, 6) == "ttyUSB") || (tmp.substr(0, 6) == "rfcomm"))
        {
//            int fd;
//            std::stringstream sPort;
//            sPort << "/dev/" << dirp->d_name;
//            fd = open(sPort.str().c_str(), O_RDWR | O_NOCTTY | O_NDELAY);
//            if (fd >= 0)
//            {
//                if (write(fd, "\r", 1) >= 0)
//                    ui->port_choice->add(dirp->d_name);
//            }
//            close(fd);
            ui->port_choice->add(dirp->d_name);
        }
    }
    closedir(dp);

#endif
}
