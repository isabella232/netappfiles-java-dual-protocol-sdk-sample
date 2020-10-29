// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package dualprotocol.sdk.sample;

import com.ea.async.Async;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.netapp.v2020_06_01.ActiveDirectory;
import com.microsoft.azure.management.netapp.v2020_06_01.SecurityStyle;
import com.microsoft.azure.management.netapp.v2020_06_01.ServiceLevel;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.AzureNetAppFilesManagementClientImpl;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.CapacityPoolInner;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.NetAppAccountInner;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.VolumeInner;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import dualprotocol.sdk.sample.common.CommonSdk;
import dualprotocol.sdk.sample.common.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.ea.async.Async.await;

public class main
{
    /**
     * Sample console application that executes CRUD management operations on Azure NetApp Files resources
     * Showcases how to create a Dual-Protocol Volume - A Volume using both NFS and SMB protocols
     * @param args
     */
    public static void main( String[] args )
    {
        Utils.displayConsoleAppHeader();

        try
        {
            Async.init();
            runAsync();
            Utils.writeConsoleMessage("Sample application successfully completed execution");
        }
        catch (Exception e)
        {
            Utils.writeErrorMessage(e.getMessage());
        }

        System.exit(0);
    }

    private static CompletableFuture<Void> runAsync()
    {
        //---------------------------------------------------------------------------------------------------------------------
        // Setting variables necessary for resources creation - change these to appropriate values related to your environment
        //---------------------------------------------------------------------------------------------------------------------
        boolean cleanup = false;

        String subscriptionId = "<subscription id>";
        String location = "eastus";
        String resourceGroupName = "anf01-rg";
        String vnetName = "vnet";
        String subnetName = "anf-sn";
        String anfAccountName = "test-account01";
        String capacityPoolName = "test-pool01";
        String capacityPoolServiceLevel = "Standard"; // Valid service levels are: Ultra, Premium, Standard
        String volumeName = "test-vol01";

        long capacityPoolSize = 4398046511104L;  // 4TiB which is minimum size
        long volumeSize = 107374182400L;  // 100GiB - volume minimum size

        // SMB/CIFS related variables
        String domainJoinUsername = "testadmin";
        String dnsList = "10.0.2.4,10.0.2.5"; // Please notice that this is a comma-separated string
        String adFQDN = "testdomain.local";
        String smbServerNamePrefix = "testsmb"; // this needs to be maximum 10 characters in length and during the domain join process a random string gets appended.

        String rootCACertFullFilePath = "ad-server.cer"; // Base64 encoded root ca certificate full file name, located at root of project

        //---------------------------------------------------------------------------------------------------------------------


        // Authenticating using service principal, refer to README.md file for requirement details
        ServiceClientCredentials credentials = Utils.getServicePrincipalCredentials(System.getenv("AZURE_AUTH_LOCATION"));
        if (credentials == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        // Instantiating a new ANF management client
        Utils.writeConsoleMessage("Instantiating a new Azure NetApp Files management client...");
        AzureNetAppFilesManagementClientImpl anfClient = new AzureNetAppFilesManagementClientImpl(credentials);
        anfClient.withSubscriptionId(subscriptionId);
        Utils.writeConsoleMessage("Api Version: " + anfClient.apiVersion());

        //------------------------------------------------------------------------------------------------------
        // Getting Active Directory Identity's password (from identity that has rights to domain join computers)
        //------------------------------------------------------------------------------------------------------
        System.out.println("Please type Active Directory's user password that will domain join ANF's SMB server and press [ENTER]:");
        String domainJoinUserPassword = Utils.getConsolePassword();

        String certContent = Utils.getRootCACert(rootCACertFullFilePath);
        if (certContent == null)
        {
            return CompletableFuture.completedFuture(null);
        }
        String encodedCertContent = Base64.getEncoder().encodeToString(certContent.getBytes());

        //---------------------------
        // Creating ANF resources
        //---------------------------

        //---------------------------
        // Create ANF Account
        //---------------------------
        Utils.writeConsoleMessage("Creating Azure NetApp Files Account...");

        String[] accountParams = {resourceGroupName, anfAccountName};
        NetAppAccountInner anfAccount = await(CommonSdk.getResourceAsync(anfClient, accountParams, NetAppAccountInner.class));
        if (anfAccount == null)
        {
            // Setting up Active Directories Object
            ActiveDirectory activeDirectory = new ActiveDirectory();
            activeDirectory.withUsername(domainJoinUsername);
            activeDirectory.withPassword(domainJoinUserPassword);
            activeDirectory.withDns(dnsList);
            activeDirectory.withDomain(adFQDN);
            activeDirectory.withSmbServerName(smbServerNamePrefix);
            activeDirectory.withServerRootCACertificate(encodedCertContent);

            NetAppAccountInner newAccount = new NetAppAccountInner();
            newAccount.withLocation(location);
            newAccount.withActiveDirectories(Collections.singletonList(activeDirectory));

            try
            {
                anfAccount = await(Creation.createANFAccount(anfClient, resourceGroupName, anfAccountName, newAccount));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating account: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Account already exists");
        }

        //---------------------------
        // Create Capacity Pool
        //---------------------------
        Utils.writeConsoleMessage("Creating Capacity Pool...");

        String[] poolParams = {resourceGroupName, anfAccountName, capacityPoolName};
        CapacityPoolInner capacityPool = await(CommonSdk.getResourceAsync(anfClient, poolParams, CapacityPoolInner.class));
        if (capacityPool == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevel));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPool = await(Creation.createCapacityPool(anfClient, resourceGroupName, anfAccountName, capacityPoolName, newCapacityPool));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating capacity pool: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Capacity Pool already exists");
        }

        //---------------------------
        // Create Volume
        //---------------------------
        Utils.writeConsoleMessage("Creating Volume with dual protocol...");

        String[] volumeParams = {resourceGroupName, anfAccountName, capacityPoolName, volumeName};
        VolumeInner volume = await(CommonSdk.getResourceAsync(anfClient, volumeParams, VolumeInner.class));
        if (volume == null)
        {
            String subnetId = "/subscriptions/" + subscriptionId + "/resourceGroups/" + resourceGroupName +
                    "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "/subnets/" + subnetName;

            List<String> protocolTypes = new ArrayList<>();
            protocolTypes.add("CIFS");
            protocolTypes.add("NFSv3");

            VolumeInner newVolume = new VolumeInner();
            newVolume.withLocation(location);
            newVolume.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevel));
            newVolume.withCreationToken(volumeName);
            newVolume.withSubnetId(subnetId);
            newVolume.withUsageThreshold(volumeSize);
            newVolume.withProtocolTypes(protocolTypes);
            newVolume.withSecurityStyle(SecurityStyle.NTFS);

            try
            {
                volume = await(Creation.createVolume(anfClient, resourceGroupName, anfAccountName, capacityPoolName, volumeName, newVolume));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating volume: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Volume already exists");
        }

        volume = await(CommonSdk.getResourceAsync(anfClient, volumeParams, VolumeInner.class));
        Utils.writeConsoleMessage("Current Volume protocol types: " + volume.protocolTypes());
        Utils.writeConsoleMessage("SMB Server FQDN: " + volume.mountTargets().get(0).smbServerFqdn());
        Utils.writeConsoleMessage("NFS IP Address: " + volume.mountTargets().get(0).ipAddress());


        //---------------------------
        // Clean up resources
        //---------------------------

        /*
          Cleanup process. For this process to take effect please change the value of
          the boolean variable 'cleanup' to 'true'
          The cleanup process starts from the innermost resources down in the hierarchy chain.
          In this case: Volume -> Capacity Pool -> Account
        */
        if (cleanup)
        {
            Utils.writeConsoleMessage("Cleaning up all created resources");

            try
            {
                await(Cleanup.runCleanupTask(anfClient, volumeParams, VolumeInner.class));
                // ARM workaround to wait for the deletion to complete
                CommonSdk.waitForNoANFResource(anfClient, volume.id(), VolumeInner.class);
                Utils.writeSuccessMessage("Volume successfully deleted: " + volume.id());

                await(Cleanup.runCleanupTask(anfClient, poolParams, CapacityPoolInner.class));
                CommonSdk.waitForNoANFResource(anfClient, capacityPool.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Primary Capacity Pool successfully deleted: " + capacityPool.id());

                await(Cleanup.runCleanupTask(anfClient, accountParams, NetAppAccountInner.class));
                CommonSdk.waitForNoANFResource(anfClient, anfAccount.id(), NetAppAccountInner.class);
                Utils.writeSuccessMessage("Account successfully deleted: " + anfAccount.id());
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while deleting resource: " + e.body().message());
                throw e;
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
